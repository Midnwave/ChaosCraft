package com.blockforge.chaoscraft.services.badges.functions;

/**
 * Parses function strings from badges.yml into BadgeFunction instances.
 *
 * Supported formats:
 * - "dummy"                                           -> DummyFunction
 * - "command:cmd_here"                                -> CommandFunction
 * - "on_mode_survive:modename"                        -> ModeSurviveFunction
 * - "above_placeholderapi:%placeholder%:value"        -> PlaceholderAboveFunction
 * - "greaterthanequal_placeholderapi:%placeholder%:value" -> PlaceholderGTEFunction
 */
public final class BadgeFunctionParser {

    private BadgeFunctionParser() {}

    /**
     * Parse a function string into the appropriate BadgeFunction.
     *
     * @param functionStr the raw function string from config
     * @return the parsed BadgeFunction, or DummyFunction if parsing fails
     */
    public static BadgeFunction parse(String functionStr) {
        if (functionStr == null || functionStr.isEmpty() || functionStr.equalsIgnoreCase("dummy")) {
            return new DummyFunction();
        }

        // command:cmd_here
        if (functionStr.startsWith("command:")) {
            String command = functionStr.substring("command:".length());
            return new CommandFunction(command);
        }

        // on_mode_survive:modename
        if (functionStr.startsWith("on_mode_survive:")) {
            String modeName = functionStr.substring("on_mode_survive:".length());
            return new ModeSurviveFunction(modeName);
        }

        // above_placeholderapi:%placeholder%:value
        if (functionStr.startsWith("above_placeholderapi:")) {
            String rest = functionStr.substring("above_placeholderapi:".length());
            return parsePlaceholderFunction(rest, true);
        }

        // greaterthanequal_placeholderapi:%placeholder%:value
        if (functionStr.startsWith("greaterthanequal_placeholderapi:")) {
            String rest = functionStr.substring("greaterthanequal_placeholderapi:".length());
            return parsePlaceholderFunction(rest, false);
        }

        // Unknown function type — fallback to dummy
        return new DummyFunction();
    }

    /**
     * Parse the placeholder:value portion of a PAPI-based function string.
     * The placeholder may contain colons (e.g. %some:complex:placeholder%),
     * so we find the last colon as the value separator.
     */
    private static BadgeFunction parsePlaceholderFunction(String rest, boolean strictlyAbove) {
        int lastColon = rest.lastIndexOf(':');
        if (lastColon <= 0 || lastColon == rest.length() - 1) {
            return new DummyFunction();
        }

        String placeholder = rest.substring(0, lastColon);
        String valueStr = rest.substring(lastColon + 1);

        try {
            double value = Double.parseDouble(valueStr);
            if (strictlyAbove) {
                return new PlaceholderAboveFunction(placeholder, value);
            } else {
                return new PlaceholderGTEFunction(placeholder, value);
            }
        } catch (NumberFormatException e) {
            return new DummyFunction();
        }
    }
}
