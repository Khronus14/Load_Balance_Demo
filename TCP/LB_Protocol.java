package TCP;

public class LB_Protocol {
    public static final String INITIAL_REQUEST = "0";
    public static final String COUNTING = "1";
    public static final String COMPLETE = "2";
    public static final String HEALTH = "3";

    /**
     * Protocol used to read/respond to messages.
     * @param input string message to respond to
     * @return message response as a string
     */
    public static String parseString(String input) {
        // [message type] [count] [limit]
        String[] _input = input.split(" ");
        String response = null;

        switch (_input[0]) {
            case INITIAL_REQUEST -> {
                if (Integer.parseInt(_input[1]) < Integer.parseInt(_input[2])) {
                    int newCount = Integer.parseInt(_input[1]) + 1;
                    response = String.format("%s %d %s", COUNTING, newCount, _input[2]);
                } else {
                    response = String.format("%s %s %s", COMPLETE, _input[1], _input[2]);
                }
            }
            case COUNTING -> {
                int newCount = Integer.parseInt(_input[1]) + 1;
                if (newCount >= Integer.parseInt(_input[2])) {
                    response = String.format("%s %d %s", COMPLETE, newCount, _input[2]);
                } else {
                    response = String.format("%s %d %s", COUNTING, newCount, _input[2]);
                }
            }
            case COMPLETE ->
                response = String.format("%s %s %s", COMPLETE, _input[1], _input[2]);
        }
        return response;
    }
}
