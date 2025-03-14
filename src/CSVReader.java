import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * A generic CSV reader that reads CSV files and maps them to Java objects of type {@code T}.
 * <p>
 * This reader requires the CSV file to have a header row where column names match the field names in the class {@code T}.
 * Supported data types include standard primitives, wrappers, {@code String}, {@code BigDecimal}, {@code BigInteger}, {@code LocalDate},
 * {@code LocalDateTime}, {@code LocalTime}, {@code Instant}, {@code UUID}, enums, and arrays of {@code String}, {@code Integer}, and {@code Double}.
 *
 * @param <T> The type of objects to be mapped from the CSV file.
 */
public class CSVReader<T> {
    private final Class<T> type;
    private String delimiter;
    private DateTimeFormatter localDateFormatter;
    private DateTimeFormatter localDateTimeFormatter;
    private DateTimeFormatter localTimeFormatter;

    /**
     * Constructs a CSVReader for the given class type.
     *
     * @param type The class type to which CSV rows should be mapped.
     */
    public CSVReader(Class<T> type) {
        this.type = type;
    }

    /**
     * Sets the delimiter used in the CSV file.
     *
     * @param delimiter The delimiter character(s) used to separate values.
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Sets the formatter for parsing {@link LocalDate} fields.
     *
     * @param formatter The {@code DateTimeFormatter} for parsing {@code LocalDate} values.
     */
    public void setLocalDateFormatter(DateTimeFormatter formatter) {
        this.localDateFormatter = formatter;
    }

    /**
     * Sets the formatter for parsing {@link LocalDateTime} fields.
     *
     * @param formatter The {@code DateTimeFormatter} for parsing {@code LocalDateTime} values.
     */
    public void setLocalDateTimeFormatter(DateTimeFormatter formatter) {
        this.localDateTimeFormatter = formatter;
    }

    /**
     * Sets the formatter for parsing {@link LocalTime} fields.
     *
     * @param formatter The {@code DateTimeFormatter} for parsing {@code LocalTime} values.
     */
    public void setLocalTimeFormatter(DateTimeFormatter formatter) {
        this.localTimeFormatter = formatter;
    }

    /**
     * Reads a CSV file and maps it to a list of objects of type {@code T}.
     *
     * @param filePath The path to the CSV file.
     * @return A list of objects of type {@code T}, populated with CSV data.
     * @throws IOException                 If an I/O error occurs reading the file.
     * @throws ReflectiveOperationException If reflection-based object creation or field access fails.
     * @throws IllegalStateException        If the delimiter or date/time formatters have not been set.
     */
    public List<T> readCSV(String filePath) throws IOException, ReflectiveOperationException {
        if (delimiter == null) throw new IllegalStateException("Delimiter must be set before reading the CSV.");
        if (localDateFormatter == null || localDateTimeFormatter == null || localTimeFormatter == null) {
            throw new IllegalStateException("Date and time formatters must be set before reading the CSV.");
        }

        List<String> lines = Files.readAllLines(Paths.get(filePath));
        if (lines.isEmpty()) throw new IOException("The CSV file is empty.");

        String[] headers = lines.get(0).split(delimiter);
        List<T> dataList = new ArrayList<>();

        for (String line : lines.subList(1, lines.size())) {
            String[] values = line.split(delimiter);
            T obj = type.getDeclaredConstructor().newInstance();

            for (int i = 0; i < headers.length; i++) {
                String fieldName = headers[i].trim();
                String fieldValue = i < values.length ? values[i].trim() : null;

                try {
                    Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(obj, parseValue(field.getType(), fieldValue));
                } catch (NoSuchFieldException ignored) {
                    // Ignore fields that do not exist in the class
                }
            }
            dataList.add(obj);
        }
        return dataList;
    }

    /**
     * Parses a string value into the appropriate data type.
     *
     * @param fieldType The target field type.
     * @param value     The string value to be parsed.
     * @return The parsed object of the specified type, or {@code null} if the value is empty.
     * @throws IllegalArgumentException If parsing fails for the given type.
     */
    private Object parseValue(Class<?> fieldType, String value) {
        if (value == null || value.isEmpty()) return null;

        try {
            if (fieldType == Integer.class) return Integer.valueOf(value);
            if (fieldType == Double.class) return Double.valueOf(value);
            if (fieldType == Boolean.class) return Boolean.valueOf(value);
            if (fieldType == Long.class) return Long.valueOf(value);
            if (fieldType == Float.class) return Float.valueOf(value);
            if (fieldType == Short.class) return Short.valueOf(value);
            if (fieldType == Byte.class) return Byte.valueOf(value);
            if (fieldType == Character.class) return value.charAt(0);
            if (fieldType == BigDecimal.class) return new BigDecimal(value);
            if (fieldType == BigInteger.class) return new BigInteger(value);
            if (fieldType == LocalDate.class) return LocalDate.parse(value, localDateFormatter);
            if (fieldType == LocalDateTime.class) return LocalDateTime.parse(value, localDateTimeFormatter);
            if (fieldType == LocalTime.class) return LocalTime.parse(value, localTimeFormatter);
            if (fieldType == Instant.class) return Instant.parse(value);
            if (fieldType == UUID.class) return UUID.fromString(value);
            if (fieldType.isEnum()) return Enum.valueOf((Class<? extends Enum>) fieldType, value);
            if (fieldType == String[].class) return value.split(";");
            if (fieldType == Integer[].class) return Arrays.stream(value.split(";")).map(Integer::valueOf).toArray(Integer[]::new);
            if (fieldType == Double[].class) return Arrays.stream(value.split(";")).map(Double::valueOf).toArray(Double[]::new);

            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing value '" + value + "' for type " + fieldType.getSimpleName(), e);
        }
    }
}
