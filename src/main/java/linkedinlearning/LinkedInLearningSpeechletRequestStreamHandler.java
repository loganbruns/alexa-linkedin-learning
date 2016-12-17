package linkedinlearning;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

/**
 * This class is created by the Lambda environment when a request comes in. All calls will be
 * dispatched to the Speechlet passed into the super constructor.
 */
public final class LinkedInLearningSpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {
    private static final Set<String> supportedApplicationIds;

    static {
        supportedApplicationIds = new HashSet<String>();
        supportedApplicationIds.add("amzn1.ask.skill.2b8456c6-5766-4945-85e0-b0148788dae6");
    }

    public LinkedInLearningSpeechletRequestStreamHandler() {
        super(new LinkedInLearningSpeechlet(), supportedApplicationIds);
    }
}
