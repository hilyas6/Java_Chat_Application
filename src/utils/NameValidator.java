package utils;
import java.util.regex.Pattern;

/**
 * Utility class for validating user IDs or names.
 */
public class NameValidator {
	 
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z]+([_-]?[a-zA-Z]+)*$");

    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }
}