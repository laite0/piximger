package main.java.client;

import main.java.annotations.Internal;

import java.io.File;

@Deprecated
//prototype
public final class PixivNameResolver {
    private StringBuffer buffer;
    private final String raw;
    private String[] name = new String[4];
    private int pointer = 0;
    private boolean finished = false;
    private State mode = State.Plain;


    private int _count = 0;

    ////one per each class to improve readability

    //state of state-machine
    private enum State {
        Plain,
        BraceStart,
        HasUnderScore,
        BraceEnd,
        Separation,
        Terminated
    }

    //token accepted by the resolver
    private static class TOKEN {
        private static final char    L_BRACE = '{',
                                    R_BRACE = '}',
                                    SEPARATOR = '_';
    }

    ////

    /**
     * Create a parser at it's initial stage, throw an {@link IllegalArgumentException} when input is
     * not parsable.
     * @param fn filename of the file, including extension.
     */
    public PixivNameResolver(String fn) {
        this.buffer = new StringBuffer(fn.length());
        this.raw = removeEx(fn, false);
    }

    /**
     * Create a parser at it's initial stage from a {@link File} object, throw
     * an {@link IllegalArgumentException} when input is not parsable.
     * @param imgLink {@link File} handle of the file.
     */
    public PixivNameResolver(File imgLink) {
        this.buffer = new StringBuffer(imgLink.getName().length());
        this.raw = removeEx(imgLink.getName(), imgLink.isDirectory());
    }

    private void advance() {
        if(pointer < raw.length() - 1)
            pointer++;
    }

    //parse ch
    private void parse(PixivNameResolver this, char ch) {
        if(_count > 3) throw new IllegalArgumentException("Invalid Filename!");
        char current = this.raw.charAt(pointer);
        switch (this.mode) {
            case Plain -> {
                switch (current) {
                    case TOKEN.SEPARATOR -> {
                        mode = State.Separation;
                        put();
                    }
                    case TOKEN.L_BRACE -> mode = State.BraceStart;
                    default -> buffer.append(ch);
                }
            }
            case Separation -> {
                switch (current) {
                    case TOKEN.L_BRACE -> mode = State.BraceStart;
                    case TOKEN.SEPARATOR -> {
                        assert buffer.toString().equals("");
                        put();
                    }
                    default -> {
                        mode = State.Plain;
                        buffer.append(ch);
                    }
                }
            }
            case BraceStart -> {
                mode = State.HasUnderScore;
                buffer.append(ch);
            }
            case HasUnderScore -> {
                if (current == TOKEN.R_BRACE) {
                    mode = State.BraceEnd;
                } else {
                    buffer.append(ch);
                }
            }
            case BraceEnd -> {
                if (current == TOKEN.SEPARATOR) {
                    mode = State.Separation;
                    put();
                } else {
                    throw new IllegalArgumentException("Invalid File Name!");
                }
            }
            case Terminated -> {//TODO:Check ?
                }
        }
        checkEnd();
    }

    //put content of the buffer(component of file name) into array
    private void put() {
        name[_count] = buffer.toString();
        buffer.setLength(0);
        _count++;
    }

    //set to finish stage
    private void checkEnd() {
        if(pointer == raw.length() - 1) {
            if(mode == State.HasUnderScore) throw new IllegalArgumentException("Invalid File Name!");
            mode = State.Terminated;
            name[_count] = buffer.toString();
            finished = true;
        }
    }

    private void checkFinished() {
        if(!finished)
            throw new UnsupportedOperationException(this.getClass().getSimpleName() + ": Must parse filename first!");
    }

    /**
     * Resolve(parse) the file name.
     * @return this
     */
    public PixivNameResolver resolve() {
        parse(raw.charAt(pointer));
        do {
            advance();
            parse(raw.charAt(pointer));
        } while (pointer < raw.length() - 1);
        if(mode != State.Terminated || !finished || _count != 3)
            throw new IllegalArgumentException(
                (!finished ? "Resolve can not be satisfied.\n" : "") +
                (_count != 3 ? "Filename format does not meet need - Legal separator count found: " + _count : ""));
        return this;
    }

    /**
     * Resolve(parse) the file name and get the result .
     * This method serves as a shorthand of {@code r.resolve().resolved()}.
     * @return the result.
     */
    public String[] resolveAndGet() {
        this.resolve();
        return name;
    }

    /**
     * Get the result, must be called at finish stage.
     * @return the result.
     * @throws UnsupportedOperationException when parsing is not done
     */
    public String[] resolved() {
        if(finished)
            return name;
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + ": Must parse filename first!");
    }


    /**
     * Get Pixiv ID.
     * @return Pixiv ID
     */
    public int id() {
        checkFinished();
        return Integer.parseInt(name[0]);
    }

    /**
     * Get title.
     * @return title
     */
    public String title() {
        checkFinished();
        return name[1];
    }

    /**
     * Get ID of the author.
     * @return author ID
     */
    public int authorID() {
        checkFinished();
        return Integer.parseInt(name[2]);
    }

    /**
     * Get name of the author.
     * @return author's name
     */
    public String authorName() {
        checkFinished();
        return name[3];
    }

    @Internal
    public static String removeEx(String fileName, boolean isDir) {
        if(isDir)
            return fileName;
        if (fileName.indexOf(".") > 0) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return fileName;
        }

    }
}
