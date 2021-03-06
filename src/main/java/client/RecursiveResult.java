package main.java.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public /*sealed*/ interface RecursiveResult /*permits Layer, Str*/ {
    Object value();
    record Layer(RecursiveResult... value) implements RecursiveResult {
        public Layer {
            Objects.requireNonNull(value);
        }

        public final String[] flattened() {
            var strList = new ArrayList<String>(value.length);
            parse(strList, value);
            return strList.toArray(String[]::new);
        }

        public final int length() {
            return value.length;
        }

        static void parse(ArrayList<String> strList, RecursiveResult[] value) {
            for (var r : value) {
                if (r instanceof Str s) strList.add(s.value);
                else if (r instanceof Layer l) parse(strList, l.value);
                else throw new IllegalArgumentException("?? Type " + r.getClass().toString());
            }
        }

        @Override
        public String toString() {
            var sb = new StringBuilder(value.length * 9);//a typical factor of tw and pixiv raw fn
            for (var r : value) {
                if (r instanceof Str s) sb.append(s.value);
                else if (r instanceof Layer l) sb.append('{').append(l.toString()).append('}');
                else throw new IllegalArgumentException("?? Type " + r.getClass().toString());
                sb.append('_');
            }
            return sb.deleteCharAt(sb.length() - 1).toString();
        }
    }

    record Str(String value) implements RecursiveResult {
        public String toString() {
            return value;
        }
    }

    //TestOnly
    static void main(String[] args) {
        Layer l = new Layer
                (
                        new Str("777"),
                        new Layer(
                            new Str("hhhhhhh"),
                            new Layer(
                                new Str("2c"),
                                new Str("rtg")
                            )
                        ),
                        new Str("888"),
                        new Layer(
                            new Str("--------------"),
                            new Str("mm")
                        )
                );
        System.out.println(Arrays.toString(l.flattened()));
        System.out.println(l.toString());
    }
}
