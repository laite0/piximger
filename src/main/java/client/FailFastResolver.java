package main.java.client;

import main.java.annotations.Internal;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class FailFastResolver {
    private       StringBuffer buffer;
    private final String       raw;
    private       String[]     name;
    private       int          pointer      = 0;
    private       boolean      finished     = false;
    private       State        mode         = State.Separation;
    private       int          segmentCount = 1;
    private       boolean      test         = true;
    private       int          countOf_     = 0;

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
     * Create a parser at it's final(finished) stage, throw an {@link IllegalArgumentException}
     * when input is not parsable.
     * @param fn filename, including extension.
     * @param isDir is a directory
     * @return A parser at it's final stage.
     */
    public static FailFastResolver less(String fn, boolean isDir) {
        String rE = removeEx(fn, isDir);
        return !rE.contains("_") ? new FailFastResolver(rE, null).resolve() : new FailFastResolver(rE).resolve();
    }

    /**
     * Create a parser at it's initial stage, throw an {@link IllegalArgumentException} when input is
     * not parsable.
     * @param name NAME of the file, extension not included.
     */
    public FailFastResolver(String name) {
        name += "\r";
        this.buffer = new StringBuffer();
        this.raw = name;
        verify();
    }

    //see ::less
    private FailFastResolver(String name, Void oneSeg) {
        raw = name + "\r";
        segmentCount = 1;
        countOf_ = 0;
        pointer = raw.length() - 1;
        test = false;
        finished = true;
        this.name = new String[] {name};
        mode = State.Terminated;
    }

    /**
     * Create a parser at it's initial stage from a {@link File} object, throw
     * an {@link IllegalArgumentException} when input is not parsable.
     * @param imgLink {@link File} handle of the file.
     */
    public FailFastResolver(File imgLink) {
        this(removeEx(imgLink.getName(), imgLink.isDirectory()));
    }

    /**
     * Create a parser at it's initial stage from a {@link Path} , throw
     * an {@link IllegalArgumentException} when input is not parsable.
     * @param imgLink {@link Path} of the file.
     */
    public FailFastResolver(Path imgLink) throws IllegalArgumentException {
        this(removeEx(imgLink.getFileName().toString(), Files.isDirectory(imgLink)));
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
        assert pointer < raw.length();
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
        throw new IllegalArgumentException("\033[0;31m" + cause + "\nAt " + pointer + ": " + raw.substring(0, pointer == 0 ? 0 : pointer - 1) + "\033[1;00m" + (raw.charAt(pointer) != '\r' ? raw.charAt(pointer) : 'E') + "\033[0;31m" + raw.substring(pointer + 1) + "\n\n   '" + (raw.charAt(pointer) != '\r' ? raw.charAt(pointer) : 'E') + "' found; " + expectation + " expected.");
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
    public FailFastResolver requireLength(int must_be) {
        assert !test;
        if (must_be != segmentCount)
            throw new IllegalArgumentException("Actual length not correspond to required length. \n Expected: " + must_be + "  Found: " + segmentCount + "  . On: " + raw);
        return this;
    }

    /**
     * Print a message if count of component of the file name doesn't match the given number.
     * @param should_be count of component that the file name should satisfy.
     * @return this
     */
    public FailFastResolver suggestLength(int should_be) {
        assert !test;
        if (should_be != segmentCount)
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
     * Validate if components match the given criteria. Returns an sequential ordered stream
     * containing validation result. {@link Boolean#TRUE} if the component at its index matches
     * corresponding criterion or either the the criterion is out-of-index or leaves
     * {@code null}. {@link Boolean#FALSE} if the component at its index does not matches
     * corresponding criterion.
     * @param criteria the criteria against which the components match
     * @return an sequential ordered stream containing validation result.
     */
    public Stream<Boolean> validate(List<Predicate<? super String>> criteria) {
        return IntStream.range(0, name.length).mapToObj(i -> {Predicate<? super String> p;return null == (p = i < criteria.size() ? criteria.get(i) : null) || p.test(name[i]);});
    }

    /**
     * Resolve(parse) the file name.
     * @return this
     */
    public FailFastResolver resolve() {
        do {
            parse(raw.charAt(pointer));
        } while (!finished);
        return this;
    }

    /**
     * Resolve(parse) the file name and get the result .
     * This method serves as a shorthand of {@code r.resolve().resolved()}.
     * @return the result.
     */
    public String[] resolveAndGet() {
        assert !test;
        this.resolve();
        return name;
    }

    /**
     * Get the result, must be called at finish stage.
     * @return the result.
     * @throws UnsupportedOperationException when parsing is not done
     */
    public String[] resolved() {
        if (finished) return name;
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
