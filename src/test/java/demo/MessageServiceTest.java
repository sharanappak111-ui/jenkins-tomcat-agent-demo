package demo;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MessageServiceTest {
    @Test
    public void returnsDefaultMessageForNull() {
        assertEquals("Hello, Jenkins!", new MessageService().message(null));
    }

    @Test
    public void returnsPersonalizedMessage() {
        assertEquals("Hello, DevOps!", new MessageService().message("DevOps"));
    }
}
