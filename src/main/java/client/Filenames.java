package main.java.client;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

public final class Filenames {

    public static final BiMap<String, String> HALF_TO_FULL_LOOKUP = HashBiMap.create(Map.ofEntries(
            entry("\\", "＼"),//BSLSH
            entry("/", "／"),//SLASH
            entry(":", "："),//SEMIC
            entry("?", "？"),//QUOTE
            entry("\"", "＂"),//QUOTM
            entry("<", "＜"),//SMLTH
            entry(">", "＞"),//LRGTH
            entry("*", "＊"),//ASTAR
            entry("|", "｜")//PIPS
    ));
    private static final BiMap<String, String> FULL_TO_HALF_LOOKUP = HALF_TO_FULL_LOOKUP.inverse();

    /**
     * map escape code to escaped chars
     */
    public static final BiMap<String, String> CODE_TO_FULL_LOOKUP = HashBiMap.create(Map.ofEntries(
            entry("=BSLSH", "＼"),
            entry("=SLASH", "／"),
            entry("=SEMIC", "："),
            entry("=QUOTE", "？"),
            entry("=QUOTM", "＂"),
            entry("=SMLTH", "＜"),
            entry("=LRGTH", "＞"),
            entry("=ASTAR", "＊"),
            entry("=PIPS", "｜")
    ));
    private static final BiMap<String, String> FULL_TO_CODE_LOOKUP = CODE_TO_FULL_LOOKUP.inverse();

    public static final Map<String, String> ABBR_CODE_TO_FULL = Map.of(
            "=HTPS", "https://"
    );

    //special =CR=LF=LF -> /r/n/r/n
    public static final List<String> ESCAPE_CODES = List.of(
            "=SEMIC", "=ASTAR", "=QUOTE", "=SLASH", "=BSLSH", "=CR=LF", "=PIPS",
            "=LF", "=XOR", "=QBR", "=SMLTH", "=LRGTH", "=QUOTM");

    public static final List<String> ABBR_CODES = List.of("=HTPS");

    public static final Set<String> STATIC_IMG_EXT = Set.of("jpg", "jpeg", "png");

    public static final Set<String> ANIMATE_IMG_EXT = Set.of("gif");

    //holding years to validate input strings
    public static final Set<String> YEARS = Set.of("2014", "2015", "2016", "2017", "2018", "2019", "2020", "2021");

    //holding years to validate parse results
    public static final Set<Integer> NUMERIC_YEARS = Set.of(2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021);

    private Filenames() {
        throw new InternalError();
    }
}