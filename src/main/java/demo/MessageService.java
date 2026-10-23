package demo;

public class MessageService {
    public String message(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Hello, Jenkins!";
        }
        return "Hello, " + name.trim() + "!";
    }
}
