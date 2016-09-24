package site.kiselev.test;


import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class App {


    /**
     * Дельта аггрегации времени минутах.
     */
    private static final int AGGREGATION_DELTA = 5;

    /**
     * Имя файла для обработки.
     */
    private static final String FILE_NAME = "sample_installs.csv";

    /**
     * Формат, по которому парсится дата
     * Пример даты 26/Oct/2015:00:00:28 +0300
     */
    private static String DATE_PARSE_FORMAT = "dd/MMM/yyyy:HH:mm:ss Z";

    /**
     * Хэш для накопления результата
     */
    private Map<String, Long> result = new TreeMap<>();

    /**
     * Функция для парсинга каждой строки и накопления результата.
     */
    private final Consumer<String> parseFunc = (s) -> {
        String[] line = parseLine(s);
        try {
            // 127.0.0.1; 26/Oct/2015:00:00:29 +0300; tracker.internal; /api/postback; 200; 3; 0.034 [0.000];
            // app_id=id12345&click_time=&install_time=2015-10-25+20%3A59%3A33.861&campaign=&
            // country_code=ES&city=None&ip_address=83.60.71.80&language=es-ES&device_name=iPad&
            // device_type=iPad3%3B1&app_version=&media_source= ; -

            // Парсим необходимые данные
            long date = parseDate(line[1]);
            Map<String, String> query = parseQuery(line[7]);
            String name = query.get("app_id");
            if (name == null) name = "";
            String country = query.get("country_code");
            if (country == null) country = "";

            // Собираем ключ
            String key = Long.toString(date) + ";" + name + ";" + country + ";";

            // Увеличиваем счетчик
            if (result.containsKey(key)) {
                result.put(key, result.get(key) + 1);
            } else {
                result.put(key, 1L);
            }

        } catch (ParseException e) {
            System.err.println("Error in date parsing: " + s);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Error in query parsing: " + s);
        }
    };


    public static void main(String[] args) {
        App app = new App();
        app.forEachLine(FILE_NAME, app.parseFunc);

        for (String key : app.result.keySet()) {
            System.out.println(key + Long.toString(app.result.get(key)));
        }
    }


    /**
     * Для каждой строки из файла filename выполняется функция func
     * Можно переделать под любой формат файла (или URL)
     * @param filename  -
     * @param func      -
     */
    private void forEachLine(String filename, Consumer<String> func) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(filename), StandardCharsets.UTF_8))){
            String line;
            while ((line = reader.readLine()) != null) {
                func.accept(line);
            }
        } catch (IOException e) {
            System.err.println("Can't read from file " + filename);
        }
    }

    /**
     * Делит строку запроса на отдельные ключ-значение
     *
     * @param string -
     * @return    -
     * @throws UnsupportedEncodingException -
     */
    private static Map<String, String> parseQuery(String string) throws UnsupportedEncodingException {

        final Map<String, String> query_pairs = new LinkedHashMap<>();
        final String[] pairs = string.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : "";
            query_pairs.put(key.trim(), value.trim());
        }
        return query_pairs;
    }

    private static String[] parseLine(String str) {
        return str.split("; ");
    }

    /**
     * Парсит дату в соответствии с форматом, аггрегирует всё до AGGREGATION_DELTA,
     * Возвращает выравненное по дельте время в секундах
     *
     * @param string    -
     * @return          -
     * @throws ParseException -
     */
    private static long parseDate(String string) throws ParseException {
        SimpleDateFormat f = new SimpleDateFormat(DATE_PARSE_FORMAT);
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(f.parse(string));
        int minutes = calendar.get(Calendar.MINUTE);
        int aggregatedMinutes = minutes % AGGREGATION_DELTA;
        calendar.set(Calendar.MINUTE, aggregatedMinutes);
        calendar.set(Calendar.SECOND, 0);

        return calendar.getTimeInMillis()/1000;
    }



}
