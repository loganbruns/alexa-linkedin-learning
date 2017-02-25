package linkedinlearning;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;

/**
 *
 * LinkedIn Learning Alex Skill adapted from SavvyConsumer sample.
 *
 * <ul>
 * <li><b>Web service</b>: Communicate with an the Amazon associates API to get best seller
 * information using aws-lib</li>
 * <li><b>Pagination</b>: Handles paginating a list of responses to avoid overwhelming the customer.
 * </li>
 * <li><b>Custom slot type</b>: demonstrates using custom slot types to handle a finite set of known values</li>
 * <li><b>Dialog and Session state</b>: Handles two models, both a one-shot ask and tell model, and
 * a multi-turn dialog model. If the user provides an incorrect slot in a one-shot model, it will
 * direct to the dialog model. See the examples section for sample interactions of these models.</li>
 * <li><b>SSML</b>: Using SSML tags to control how Alexa renders the text-to-speech</li>
 * </ul>
 * <p>
 * <h2>Examples</h2>
 * <p>
 * <b>Dialog model</b>
 * <p>
 * User: "Alexa, open LinkedIn Learning"
 * <p>
 * Alexa: "Welcome to the LinkedIn Learning. For which category do you want to hear the popular content?"
 * <p>
 * User: "courses"
 * <p>
 * Alexa: "Getting the popular courses. The most popular course is .... Would you like to
 * hear more?"
 * <p>
 * User: "yes"
 * <p>
 * Alexa: "Second ... Third... Fourth... Would you like to hear more?"
 * <p>
 * User : "no"
 * <p>
 * <b>One-shot model</b>
 * <p>
 * User: "Alexa, ask LinkedIn Learning for popular courses"
 * <p>
 * Alexa: "Getting the popular courses. The most popular course is .... Would you like to
 * hear more?"
 * <p>
 * User: "No"
 */
public class LinkedInLearningSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(LinkedInLearningSpeechlet.class);

    /**
     * The key to find the current index from the session attributes.
     */
    private static final String SESSION_CURRENT_INDEX = "current";

    /**
     * The key to find the current category from the session attributes.
     */
    private static final String SESSION_CURRENT_CATEGORY = "category";

    /**
     * The Max number of items for Alexa to read from a request to Amazon.
     */
    private static final int MAX_ITEMS = 10;

    /**
     * The number of items read for each pagination request, until we reach the MAX_ITEMS.
     */
    private static final int PAGINATION_SIZE = 3;

    /**
     * The Category slot.
     */
    private static final String SLOT_CATEGORY = "Category";
    private static final String SLOT_TOPIC = "Topic";
    private static final String SLOT_SOFTWARE = "Software";

    /**
     * A Mapping of alternative ways a user will say a category to how Amazon has defined the
     * category. Use a tree map so gets can be case insensitive.
     */
    private static final Map<String, String> spokenNameToCategory = new TreeMap<String, String>(
            String.CASE_INSENSITIVE_ORDER);

    static {
        spokenNameToCategory.put("courses", "COURSE");
        spokenNameToCategory.put("course", "COURSE");
        spokenNameToCategory.put("videos", "VIDEO");
        spokenNameToCategory.put("video", "VIDEO");
        spokenNameToCategory.put("learning paths", "LEARNING_PATH");
        spokenNameToCategory.put("learning path", "LEARNING_PATH");
        spokenNameToCategory.put("paths", "LEARNING_PATH");
        spokenNameToCategory.put("path", "LEARNING_PATH");
    }

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        String speechOutput =
          "Welcome to Linked In Learning. You can ask about a particular topic or ask for the most popular content in a category.";
        String repromptText = "Please choose a category by saying, " +
                "courses <break time=\"0.2s\" /> " +
                "videos <break time=\"0.2s\" /> " +
                "learning paths";

        // Here we are prompting the user for input
        return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true);
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("TopSellers".equals(intentName)) {
            return getTopSellers(intent, session);
        } else if ("TeachMe".equals(intentName)) {
            return teachMe(intent, session);
        } else if ("HearMore".equals(intentName)) {
            return getNextPageOfItems(intent, session);
        } else if ("DontHearMore".equals(intentName)) {
            PlainTextOutputSpeech output = new PlainTextOutputSpeech();
            output.setText("");
            return SpeechletResponse.newTellResponse(output);
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelp();
        } else if ("AMAZON.StopIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else if ("AMAZON.CancelIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else {
            throw new SpeechletException("Invalid Intent: " + intentName);
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any cleanup logic goes here
    }

    /**
     * Calls Learning API to get the top content for a given category. Then Creates a
     * {@code SpeechletResponse} for the intent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     * @throws SpeechletException
     * @see <a href="https://www.linkedin.com/learning/">LinkedIn Learning API </a>
     */
    private SpeechletResponse getTopSellers(final Intent intent, final Session session)
            throws SpeechletException {
        String repromptText = "";

        // Check if we are in a session, and if so then reprompt for yes or no
        if (session.getAttributes().containsKey(SESSION_CURRENT_INDEX)) {
            String speechOutput = "Would you like to hear more?";
            repromptText = "Would you like to hear more popular ones? Please say yes or no.";
            return newAskResponse(speechOutput, false, repromptText, false);
        }

        Slot categorySlot = intent.getSlot(SLOT_CATEGORY);

        // Find the lookup word for the given category.
        String lookupCategory = getLookupWord(categorySlot);
        if (lookupCategory == null)
          lookupCategory = "COURSE";

        // Remove the periods to fix things like d. v. d.s to dvds
        String category = categorySlot.getValue().replaceAll("\\.\\s*", "");

        if (lookupCategory != null) {
          List<String> items = fetchTitles(lookupCategory, "");

            // Configure the card and speech output.
            String cardTitle = "Popular in " + category;
            StringBuilder cardOutput = new StringBuilder();
            cardOutput.append("Popular in ").append(category).append(" are: ");
            StringBuilder speechOutput = new StringBuilder();
            speechOutput.append("Here are the popular in ").append(category).append(". ");
            session.setAttribute(SESSION_CURRENT_CATEGORY, category);

            // Iterate through the response and set the intial response, as well as the
            // session attributes for pagination.
            int i = 0;
            for (String item : items) {
                int numberInList = i + 1;
                if (numberInList == 1) {
                    // Set the speech output and current index for just the top item in the list.
                    // Other results are paginated based on subsequent user intents
                    speechOutput.append("The most popular is: ").append(item).append(". ");
                    session.setAttribute(SESSION_CURRENT_INDEX, numberInList);
                }

                // Set the session attributes and full card output
                session.setAttribute(Integer.toString(i), item);
                cardOutput.append(numberInList).append(". ").append(item).append(".");
                i++;
            }

            if (i == 0) {
                // There were no items returned for the specified item.
                SsmlOutputSpeech output = new SsmlOutputSpeech();
                output.setSsml("<speak>I'm sorry, I cannot get the popular in " + category
                        + " at this time. Please try again later. Goodbye.</speak>");
                return SpeechletResponse.newTellResponse(output);
            }

            speechOutput.append(" Would you like to hear the rest?");
            repromptText = "Would you like to hear the rest? Please say yes or no.";

            SimpleCard card = new SimpleCard();
            card.setContent(cardOutput.toString());
            card.setTitle(cardTitle);

            SpeechletResponse response = newAskResponse("<speak>" + speechOutput.toString() + "</speak>", true,
                    repromptText, false);
            response.setCard(card);

            return response;
        } else {

            // The category didn't match one of our predefined categories. Reprompt the user.
            String speechOutput = "I'm not sure what the category is, please try again";
            repromptText =
                    "I'm not sure what the category is, you can say " +
                "courses <break time=\"0.2s\" /> " +
                "videos <break time=\"0.2s\" /> " +
                "learning paths.";
            return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true);
        }
    }

    /**
     * Calls Learning API to get content for a given category and keywords. Then Creates a
     * {@code SpeechletResponse} for the intent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     * @throws SpeechletException
     * @see <a href="https://www.linkedin.com/learning/">LinkedIn Learning API </a>
     */
    private SpeechletResponse teachMe(final Intent intent, final Session session)
            throws SpeechletException {
        String repromptText = "";

        // Check if we are in a session, and if so then reprompt for yes or no
        if (session.getAttributes().containsKey(SESSION_CURRENT_INDEX)) {
            String speechOutput = "Would you like to hear more?";
            repromptText = "Would you like to hear more ones? Please say yes or no.";
            return newAskResponse(speechOutput, false, repromptText, false);
        }

        Slot categorySlot = intent.getSlot(SLOT_CATEGORY);

        // Find the lookup word for the given category.
        String lookupCategory = getLookupWord(categorySlot);
        if (lookupCategory == null)
          lookupCategory = "COURSE";

        // Remove the periods to fix things like d. v. d.s to dvds
        String category;
        if ((categorySlot != null) && (categorySlot.getValue() != null))
          category = categorySlot.getValue().replaceAll("\\.\\s*", "");
        else
          category = "courses";

        String keywords = null;
        Slot keywordSlot = intent.getSlot(SLOT_TOPIC);
        if ((keywordSlot != null) && (keywordSlot.getValue() != null))
          keywords = keywordSlot.getValue();
        else {
          keywordSlot = intent.getSlot(SLOT_SOFTWARE);
          if ((keywordSlot != null) && (keywordSlot.getValue() != null))
            keywords = keywordSlot.getValue();
        }

        if (lookupCategory != null) {
          List<String> items = fetchTitles(lookupCategory, keywords);

          // Configure the card and speech output.
          String cardTitle = "Popular " + category + " about " + keywords;
            StringBuilder cardOutput = new StringBuilder();
            cardOutput.append("Here are the ").append(category).append(" about ").append(keywords).append(": ");
            StringBuilder speechOutput = new StringBuilder();
            speechOutput.append("Here are the ").append(category).append(" about ").append(keywords).append(".");
            session.setAttribute(SESSION_CURRENT_CATEGORY, category);

            // Iterate through the response and set the intial response, as well as the
            // session attributes for pagination.
            int i = 0;
            for (String item : items) {
                int numberInList = i + 1;
                if (numberInList == 1) {
                    // Set the speech output and current index for just the top item in the list.
                    // Other results are paginated based on subsequent user intents
                    speechOutput.append("The most popular is: ").append(item).append(". ");
                    session.setAttribute(SESSION_CURRENT_INDEX, numberInList);
                }

                // Set the session attributes and full card output
                session.setAttribute(Integer.toString(i), item);
                cardOutput.append(numberInList).append(". ").append(item).append(".");
                i++;
            }

            if (i == 0) {
                // There were no items returned for the specified item.
                SsmlOutputSpeech output = new SsmlOutputSpeech();
                output.setSsml("<speak>I'm sorry, I cannot get the " + category
                        + " for " + keywords + "at this time. Please try again later. Goodbye.</speak>");
                return SpeechletResponse.newTellResponse(output);
            }

            speechOutput.append(" Would you like to hear the rest?");
            repromptText = "Would you like to hear the rest? Please say yes or no.";

            SimpleCard card = new SimpleCard();
            card.setContent(cardOutput.toString());
            card.setTitle(cardTitle);

            SpeechletResponse response = newAskResponse("<speak>" + speechOutput.toString() + "</speak>", true,
                    repromptText, false);
            response.setCard(card);

            return response;
        } else {

            // The category didn't match one of our predefined categories. Reprompt the user.
            String speechOutput = "I'm not sure what the category is, please try again";
            repromptText =
                    "I'm not sure what the category is, you can say " +
                "courses <break time=\"0.2s\" /> " +
                "videos <break time=\"0.2s\" /> " +
                "learning paths.";
            return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true);
        }
    }

    /**
     * Fetches the top ten selling titles from the Product Advertising API.
     *
     * @throws SpeechletException
     */
  private List<String> fetchTitles(String category, String keywords) throws SpeechletException {
        List<String> titles = new LinkedList<String>();
        try {
          return LinkedInLearningApiHelper.summarize(LinkedInLearningApiHelper.search(category, keywords), category);
        } catch (Exception e) {
          throw new SpeechletException(e);
        }
    }

    /**
     * Gets the 2nd-MAX_ITEMS number of titles from the session attributes.
     */
    private SpeechletResponse getNextPageOfItems(final Intent intent, final Session session) {
        if (session.getAttributes().containsKey(SESSION_CURRENT_INDEX)) {
            int currentIndex = (Integer) session.getAttribute(SESSION_CURRENT_INDEX);
            int currentItemNumberInList = currentIndex + 1;
            StringBuilder speechOutput = new StringBuilder();

            // Iterate through the session attributes to create the next n results for the user.
            for (int i = 0; i < PAGINATION_SIZE; i++) {
                String currentString =
                        (String) session.getAttribute(Integer.toString(currentIndex));
                if (currentString != null) {
                    if (currentItemNumberInList < MAX_ITEMS) {
                        speechOutput.append("<say-as interpret-as=\"ordinal\">" + currentItemNumberInList
                                + "</say-as>. " + currentString + ". ");
                    } else {
                        speechOutput.append("And the <say-as interpret-as=\"ordinal\">"
                                + currentItemNumberInList
                                + "</say-as> most popular content is. " + currentString
                                + ". Those were the 10 most popular in Linked In Learning "
                                + session.getAttribute(SESSION_CURRENT_CATEGORY) + " content");
                    }
                    currentIndex++;
                    currentItemNumberInList++;
                }
            }

            // Set the new index and end the session if the newIndex is greater than the MAX_ITEMS
            session.setAttribute(SESSION_CURRENT_INDEX, currentIndex);
            if (currentIndex < MAX_ITEMS) {
                speechOutput.append(" Would you like to hear more?");
                return newAskResponse(speechOutput.toString(), true,
                        "Would you like to hear more popular content? Please say yes or no.", false);
            } else {
                SsmlOutputSpeech output = new SsmlOutputSpeech();
                output.setSsml("<speak>" + speechOutput.toString() + "</speak>");
                return SpeechletResponse.newTellResponse(output);
            }
        } else {
            // The user attempted to get more results without ever uttering the category.
            // Reprompt the user for the proper usage.
            String speechOutput =
                    "Welcome to Linked In Learning. For which category do you want "
                            + "to hear the popular content?.";
            String repromptText = "Please choose a category by saying, " +
                "courses <break time=\"0.2s\" /> " +
                "videos <break time=\"0.2s\" /> " +
                "learning paths";
            return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true);
        }
    }

    /**
     * Gets the lookup word based on the input category slot. The lookup word will be from the
     * BROWSE_NODE_MAP and will attempt to get an exact match. However, if no exact match exists
     * then the function will check for a contains.
     *
     * @param categorySlot
     *            the input category slot
     * @returns {string} the lookup word for the BROWSE_NODE_MAP
     */
    private String getLookupWord(Slot categorySlot) {
        String lookupCategory = null;
        if (categorySlot != null && categorySlot.getValue() != null) {
            // Lower case the incoming slot and remove spaces
            String category =
                    categorySlot
                            .getValue()
                            .toLowerCase()
                            .replaceAll("\\s", "")
                            .replaceAll("\\.", "")
                            .replaceAll("three", "3");

            // Check for spoken names
            lookupCategory = spokenNameToCategory.get(category);
            if (lookupCategory == null) {
              lookupCategory = "COURSE";
            }
        }

        return lookupCategory;
    }

    /**
     * Instructs the user on how to interact with this skill.
     */
    private SpeechletResponse getHelp() {
        String speechOutput =
                "You can ask for the popular content on Linked In Learning for a given category. "
                        + "For example, get popular courses, or you can say exit. "
                        + "Now, what can I help you with?";
        String repromptText =
                "I'm sorry I didn't understand that. You can say things like," +
                "courses <break time=\"0.2s\" /> " +
                "videos <break time=\"0.2s\" /> " +
                "learning paths. Or you can say exit. Now, what can I help you with?";
        return newAskResponse(speechOutput, false, "<speak>" + repromptText + "</speak>", true);
    }

    /**
     * Wrapper for creating the Ask response from the input strings.
     * 
     * @param stringOutput
     *            the output to be spoken
     * @param isOutputSsml
     *            whether the output text is of type SSML
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @param isRepromptSsml
     *            whether the reprompt text is of type SSML
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, boolean isOutputSsml,
            String repromptText, boolean isRepromptSsml) {
        OutputSpeech outputSpeech, repromptOutputSpeech;
        if (isOutputSsml) {
            outputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) outputSpeech).setSsml(stringOutput);
        } else {
            outputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) outputSpeech).setText(stringOutput);
        }

        if (isRepromptSsml) {
            repromptOutputSpeech = new SsmlOutputSpeech();
            ((SsmlOutputSpeech) repromptOutputSpeech).setSsml(repromptText);
        } else {
            repromptOutputSpeech = new PlainTextOutputSpeech();
            ((PlainTextOutputSpeech) repromptOutputSpeech).setText(repromptText);
        }
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);
        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }
}
