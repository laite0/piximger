package main.java.client;

import main.java.annotations.Internal;
import main.java.match.algebra.Either;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileNameResolver {
    private final boolean prints;
    private boolean error;
    private   StringBuffer      buffer;
    private final String            raw;
    private       String[]          name;
    private       int               pointer      = 0;
    private       boolean           finished     = false;
    private       State mode         = State.Separation;
    private       int               segmentCount = 1;
    private       boolean           test         = true;
    private       int               countOf_     = 0;

    ////one per each class to improve readability

    //state of state-machine
    private enum State {
        Plain,
        BraceStart,
        HasUnderScore,
        BraceEnd,
        Separation,
        Terminated,
        Exited //unused
    }

    //token accepted by the resolver
    private static class TOKEN {
        private static final char
                L_BRACE     = '{',
                R_BRACE     = '}',
                SEPARATOR   = '_',
                END_SYMBOL  = '\r';
    }

    ////

    /**
     * Create a parser at it's initial stage.
     * @param name NAME of the file, extension not included.
     * @param prints does the parser prints error when first non-parsable character is encountered.
     *               When set to {@literal false}, the error is swallowed quietly and the parser
     *               will stop to false.
     */
    public FileNameResolver(String name, boolean prints) {
        this.prints = prints;
        name += "\r";
        this.buffer = new StringBuffer();
        this.raw = name;
        verify();
    }

    /**
     * Create a parser at it's initial stage from a {@link File} object.
     * @param imgLink {@link File} handle of the file.
     * @param prints does the parser prints error when first non-parsable character is encountered.
     *               When set to {@literal false}, the error is swallowed quietly and the parser
     *               will stop to false.
     */
    public FileNameResolver(File imgLink, boolean prints) {
        this(removeEx(imgLink.getName(), imgLink.isDirectory()), prints);
    }

    /**
     * Create a parser at it's initial stage from a {@link Path} .
     * @param imgLink {@link Path} of the file.
     * @param prints does the parser prints error when first non-parsable character is encountered.
     *               When set to {@literal false}, the error is swallowed quietly and the parser
     *               will stop to false.
     */
    public FileNameResolver(Path imgLink, boolean prints) throws IllegalArgumentException {
        this(removeEx(imgLink.getFileName().toString(), Files.isDirectory(imgLink)), prints);
    }

    //verify input name eagerly
    private void verify() {
        do {
            parse(raw.charAt(pointer));
        } while (test);
    }

    //set to finish stage
    private void finish() {
        finished = true;
    }

    //set to ready-to-parse stage
    private void initialize() {
        assert buffer.toString().length() == 0;
        segmentCount += countOf_;
        name = new String[segmentCount];
        assert segmentCount == (countOf_ + 1);
        countOf_ = 0;
        pointer = 0;
        mode = State.Separation;
        test = false;
        buffer = new StringBuffer();
    }

    //parse ch
    //actual input(char current, Mode mode, int pointer, StringBuffer buffer)
    private void parse(char ch) {
        if (error) return;
        char current = raw.charAt(pointer);
        switch (this.mode) {
            case Plain -> {
                switch (current) {
                    case TOKEN.SEPARATOR -> {
                        mode = State.Separation;
                        put();
                        advance();
                        countOf_++;
                    }
                    case TOKEN.L_BRACE -> {
                        if (test) error("Illegal Bracket Start", "'_' or a none-key character");
                    }
                    case TOKEN.R_BRACE -> {
                        if (test) error("Unclosed Bracket", "'_' or a none-key character");
                    }
                    case TOKEN.END_SYMBOL -> {
                        mode = State.Terminated;
                        put();
                    }
                    default -> {
                        append();
                        advance();
                    }
                }
            }
            case Terminated -> {
                assert buffer.toString().length() == 0;
                terminate();
            }
            case Separation -> {
                assert buffer.toString().length() == 0;
                switch (current) {
                    case TOKEN.L_BRACE -> {
                        mode = State.BraceStart;
                        advance();
                    }
                    case TOKEN.R_BRACE -> {
                        if (test) error("Unclosed Bracket", "'_', '{' or a none-key character");
                    }
                    case TOKEN.SEPARATOR -> {
                        put();
                        advance();
                        countOf_++;
                    }
                    case TOKEN.END_SYMBOL -> {
                        mode = State.Terminated;
                        put();
                    }
                    default -> {
                        mode = State.Plain;
                        append();
                        advance();
                    }
                }
            }
            case BraceStart -> {
                assert buffer.toString().length() == 0;
                switch (current) {
                    case TOKEN.L_BRACE -> {
                        if (test) error("Illegal Bracket Start", "'_' or a none-key character");
                    }
                    case TOKEN.R_BRACE -> {
                        if (test) error("Illegal Bracket Usage", "'_' or a none-key character");
                    }
                    case TOKEN.SEPARATOR -> {
                        mode = State.HasUnderScore;
                        append();
                        advance();
                    }
                    case TOKEN.END_SYMBOL -> {
                        if (test) error("Unclosed Bracket", "'_' or a none-key character");
                    }
                    default -> {
                        mode = State.HasUnderScore;
                        append();
                        advance();
                    }
                }
            }
            case HasUnderScore -> {
                switch (current) {
                    case TOKEN.L_BRACE -> {
                        if (test) error("Illegal Bracket Start", "'_', '}' or a none-key character");
                    }
                    case TOKEN.R_BRACE -> {
                        mode = State.BraceEnd;
                        put();
                        advance();
                    }
                    case TOKEN.SEPARATOR -> {
                        append();
                        advance();
                    }
                    case TOKEN.END_SYMBOL -> {
                        if (test) error("Unclosed Bracket", "'_', '}' or a none-key character");
                    }
                    default -> {
                        append();
                        advance();
                    }
                }
            }
            case BraceEnd -> {
                switch (current) {
                    case TOKEN.L_BRACE -> {
                        if (test) error("Illegal Bracket Start", "'_'");
                    }
                    case TOKEN.R_BRACE -> {
                        if (test) error("Illegal Bracket End", "'_'");
                    }
                    case TOKEN.SEPARATOR -> {
                        mode = State.Separation;
                        advance();
                        countOf_++;
                    }
                    case TOKEN.END_SYMBOL -> mode = State.Terminated;
                    default -> {
                        append();
                        advance();
                    }
                }
            }
        }
    }

    //put content of the buffer(component of file name) into array, ignored at verify(initial) stage
    private void put() {
        if (!test) {
            assert name.length != 0;
            name[countOf_] = buffer.toString();
            buffer.setLength(0);
        }
    }


    private void advance() {
        pointer++;
        assert pointer < raw.length();//(raw.length() + 1) - 1
    }

    //append current char to buffer
    private void append() {
        if (!test) buffer.append(raw.charAt(pointer));
    }

    //stage: initial -> ready-to-parse   parsing -> finished
    private void terminate() {
        assert pointer == raw.length() - 1;
        if (test) {
            initialize();
        } else finish();
    }

    //issue an error and stop parsing
    private void error(String cause, String expectation) {
        assert test;
        assert mode != State.Terminated;
        error = true;
        test = false;
        if (prints) System.out.println("\033[0;31m" + cause + "\nAt " + pointer + ": " + raw.substring(0, pointer == 0 ? 0 : pointer - 1) + "\033[1;00m" + (raw.charAt(pointer) != '\r' ? raw.charAt(pointer) : 'E') + "\033[0;31m" + raw.substring(pointer + 1) + "\n\n   '" + (raw.charAt(pointer) != '\r' ? raw.charAt(pointer) : 'E') + "' found; " + expectation + " expected.");
    }

    private void checkFinished() {
        if (!finished)
            throw new UnsupportedOperationException(this.getClass().getSimpleName() + ": Must parse filename first!");
    }


    /**
     * Throw an error if count of component of the file name doesn't match the given number.
     * @param must_be count of component that the file name must satisfy.
     * @return this
     */
    public FileNameResolver requireLength(int must_be) {
        assert !test;
        if (error) {
            throw new IllegalArgumentException("Name cannot be resolved!");
        } else if (must_be != segmentCount)
            throw new IllegalArgumentException("Actual length not correspond to required length. \n Expected: " + must_be + "  Found: " + segmentCount + "  . On: " + raw);
        return this;
    }

    /**
     * Print a message if count of component of the file name doesn't match the given number.
     * @param should_be count of component that the file name should satisfy.
     * @return this
     */
    public FileNameResolver suggestLength(int should_be) {
        assert !test;
        if (error) {
            System.out.println("Name cannot be resolved!");
            return this;
        } else if (should_be != segmentCount)
            System.err.println("Actual length not correspond to suggested length. \n Expected: " + should_be + "  Found: " + segmentCount + "  . On: " + raw);
        return this;
    }

    /**
     * Test if count of component of the file name match the given number.
     * @param should_be count of component that the file name should satisfy.
     * @return does it match
     */
    public boolean ofLength(int should_be) {
        assert !test;
        return should_be == segmentCount;
    }


    /**
     * @return is file name valid. Or, precisely, does name of the file can be parse by this parser.
     */
    public boolean isValid() {
        return !error;
    }

    /**
     * get 'Resolved Ratio' of the parser, indicating length of parsable content of the file name.
     * @return Resolved Ratio
     */
    public float resolvedRatio() {
        return !error ? 1 : (float) pointer / (float) raw.length();
    }

    /**
     * Get count of components of the file name.
     * @apiNote since this parser won't handle some of cases correctly, the returned number may
     * not always be expected.
     * @return count of components of the file name
     */
    public int possibleSegments() {
        return !error ? segmentCount : countOf_ + 1;
    }

    /**
     * Resolve(parse) the file name.
     * @return this
     */
    public FileNameResolver resolve() {
        do {
            parse(raw.charAt(pointer));
        } while (!finished);
        return this;
    }

    /**
     * Resolve(parse) the file name and get the result (Ideally an array inside a {@code Either}).
     * This method serves as a shorthand of {@code r.resolve().resolved()}.
     * @return the result. note this method use a {@code Either} return type, basically
     * translating to a Fail-Success result. {@code Either::left} indicating a failure and {@code Either::right}
     * indicating a success, the returned array inside the right of {@code Either<String, String[]>}
     */
    public Either<String, String[]> resolveAndGet() {
        assert !test;
        this.resolve();
        if (error)
            return Either.left("failed!");
        else return Either.right(name);
    }

    /**
     * Get the result (Ideally an array inside a {@code Either}).
     * @return the result. note this method use a {@code Either} return type, basically
     * translating to a Fail-Success result. {@code Either::left} indicating a failure and {@code Either::right}
     * indicating a success, the returned array inside the right of {@code Either<String, String[]>}
     */
    public Either<String, String[]> resolved() {
        if (finished) if (error)
            return Either.left("failed!");
        else return Either.right(name);
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ": Must parse filename first!");
    }

    @Internal
    protected static String removeEx(String fileName, boolean isDir) {
        if (isDir) return fileName;
        if (fileName.indexOf(".") > 0) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return fileName;
        }

    }

}
