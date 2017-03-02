package linkedinlearning;

import static linkedinlearning.LinkedInLearningApiHelper.Content;

import java.io.IOException;
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

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.Directive;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioItem;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioPlayer;
import com.amazon.speech.speechlet.interfaces.audioplayer.ClearBehavior;
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior;
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.ClearQueueDirective;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackFailedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackFinishedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackNearlyFinishedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackStartedRequest;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.PlaybackStoppedRequest;
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
public class LinkedInLearningSpeechlet implements Speechlet, AudioPlayer {
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
     * The key to find the current category from the session attributes.
     */
    private static final String SESSION_CURRENT_START = "offset";

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
        } else if ("HearMore".equals(intentName) || "AMAZON.NextIntent".equals(intentName)) {
            return getNext(intent, session);
        } else if ("DontHearMore".equals(intentName)) {
            PlainTextOutputSpeech output = new PlainTextOutputSpeech();
            output.setText("");
            return SpeechletResponse.newTellResponse(output);
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelp();
        } else if ("AMAZON.PauseIntent".equals(intentName)) {
	    List<Directive> directives = new LinkedList<Directive>();

	    long offset = System.currentTimeMillis();
	    String start = (String) session.getAttribute(SESSION_CURRENT_START);
	    if ((start != null) && (Long.parseLong(start) < offset)) {
		session.setAttribute(SESSION_CURRENT_START,
				     Long.toString((offset - Long.parseLong(start))));

		ClearQueueDirective clearQueue = new ClearQueueDirective();
		clearQueue.setClearBehavior(ClearBehavior.CLEAR_ALL);
		directives.add(clearQueue);
	    }

            PlainTextOutputSpeech output = new PlainTextOutputSpeech();
            output.setText("");
            SpeechletResponse response = SpeechletResponse.newTellResponse(output);
	    if (!directives.isEmpty()) {
		response.setDirectives(directives);
		response.setShouldEndSession(false);
	    }

	    return response;
        } else if ("AMAZON.ResumeIntent".equals(intentName)) {
	    session.setAttribute(SESSION_CURRENT_INDEX, ((Integer)session.getAttribute(SESSION_CURRENT_INDEX)).intValue() - 1);
	    return getNext(intent, session);
        } else if ("AMAZON.StopIntent".equals(intentName)) {
	    List<Directive> directives = new LinkedList<Directive>();

            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

	    if (session.getAttributes().containsKey(SESSION_CURRENT_START)) {
		ClearQueueDirective clearQueue = new ClearQueueDirective();
		clearQueue.setClearBehavior(ClearBehavior.CLEAR_ALL);
		directives.add(clearQueue);
	    }

            SpeechletResponse response = SpeechletResponse.newTellResponse(outputSpeech);
	    if (!directives.isEmpty()) {
		response.setDirectives(directives);
	    }

	    return response;
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

    @Override
    public SpeechletResponse onPlaybackFailed(SpeechletRequestEnvelope<PlaybackFailedRequest> requestEnvelope) {
      PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
      outputSpeech.setText("Sorry, we were not able to play the Linked In Learning audio stream.");

      return SpeechletResponse.newTellResponse(outputSpeech);
    }

    @Override
    public SpeechletResponse onPlaybackFinished(SpeechletRequestEnvelope<PlaybackFinishedRequest> requestEnvelope) {
      PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
      outputSpeech.setText("Would you like to listen to some more?");

      return SpeechletResponse.newTellResponse(outputSpeech);
    }

    @Override
    public SpeechletResponse onPlaybackNearlyFinished(SpeechletRequestEnvelope<PlaybackNearlyFinishedRequest> requestEnvelope) {
      return null;
    }

    @Override
    public SpeechletResponse onPlaybackStarted(SpeechletRequestEnvelope<PlaybackStartedRequest> requestEnvelope) {
      return null;
    }

    @Override
    public SpeechletResponse onPlaybackStopped(SpeechletRequestEnvelope<PlaybackStoppedRequest> requestEnvelope) {
      return null;
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
          List<Content> items = fetchTitles(lookupCategory, "");

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
            for (Content item : items) {
                int numberInList = i + 1;
                if (numberInList == 1) {
                    // Set the speech output and current index for just the top item in the list.
                    // Other results are paginated based on subsequent user intents
                    speechOutput.append("The most popular is: ").append(item.title).append(". ");
                    session.setAttribute(SESSION_CURRENT_INDEX, numberInList);
                }

                // Set the session attributes and full card output
                session.setAttribute(Integer.toString(i), item);
                cardOutput.append(numberInList).append(". ").append(item.title).append(".");
                i++;
            }

            if (i == 0) {
                // There were no items returned for the specified item.
                SsmlOutputSpeech output = new SsmlOutputSpeech();
                output.setSsml("<speak>I'm sorry, I cannot get the popular in " + category
                        + " at this time. Please try again later. Goodbye.</speak>");
                return SpeechletResponse.newTellResponse(output);
            }

	    speechOutput.append(" Would you like to listen to the introduction?");
            repromptText = "Would you like to listen to the introduction? Please say yes or no.";

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
          List<Content> items = fetchTitles(lookupCategory, keywords);

          // Configure the card and speech output.
          String cardTitle = "Popular " + category + " about " + keywords;
            StringBuilder cardOutput = new StringBuilder();
            cardOutput.append("Here are the ").append(category).append(" about ").append(keywords).append(": ");
            StringBuilder speechOutput = new StringBuilder();
            speechOutput.append("Here are the ").append(category).append(" about ").append(keywords);
            session.setAttribute(SESSION_CURRENT_CATEGORY, category);

            // Iterate through the response and set the intial response, as well as the
            // session attributes for pagination.
            int i = 0;
            for (Content item : items) {
                int numberInList = i + 1;
                if (numberInList == 1) {
                    // Set the speech output and current index for just the top item in the list.
                    // Other results are paginated based on subsequent user intents
                    speechOutput.append("The most popular is: ").append(item.title);
                    session.setAttribute(SESSION_CURRENT_INDEX, i);
                }

                // Set the session attributes and full card output
                session.setAttribute(Integer.toString(i), item);
                cardOutput.append(numberInList).append(". ").append(item.title).append(".");
                i++;
            }

            if (i == 0) {
                // There were no items returned for the specified item.
                SsmlOutputSpeech output = new SsmlOutputSpeech();
                output.setSsml("<speak>I'm sorry, I cannot get the " + category
                        + " for " + keywords + "at this time. Please try again later. Goodbye.</speak>");
                return SpeechletResponse.newTellResponse(output);
            }

	    speechOutput.append(" Would you like to listen to the course introduction?");
            repromptText = "Would you like to listen to the introduction? Please say yes or no.";

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
  private List<Content> fetchTitles(String category, String keywords) throws SpeechletException {
    try {
      return LinkedInLearningApiHelper.summarize(LinkedInLearningApiHelper.search(category, keywords), category);
    } catch (Exception e) {
      throw new SpeechletException(e);
    }
  }

  private SpeechletResponse getNext(final Intent intent, final Session session) {
    List<Directive> directives = new LinkedList<Directive>();

    if (session.getAttributes().containsKey(SESSION_CURRENT_INDEX)) {
      StringBuilder speechOutput = new StringBuilder();

      int currentIndex = (Integer) session.getAttribute(SESSION_CURRENT_INDEX);
      Map<String, String> item = (Map<String, String>) session.getAttribute(Integer.toString(currentIndex));
      if ((item != null) && (item.get("slug") != null)) {
	try {
	  speechOutput.append("Now playing ");
	  speechOutput.append(item.get("title"));

	  String playbackUrl = LinkedInLearningApiHelper.getPlaybackUrl(item.get("slug"));

	  Stream stream = new Stream();
	  stream.setUrl(playbackUrl);
	  stream.setToken(item.get("slug"));

	  long offset = System.currentTimeMillis();
	  String start = (String) session.getAttribute(SESSION_CURRENT_START);
	  if ((start != null) && (Long.parseLong(start) < offset)) {
	      stream.setOffsetInMilliseconds(Long.parseLong(start));
	  }

	  AudioItem audio = new AudioItem();
	  audio.setStream(stream);

	  PlayDirective play = new PlayDirective();
	  play.setAudioItem(audio);
	  play.setPlayBehavior(PlayBehavior.REPLACE_ALL);

	  directives.add(play);

	  session.setAttribute(SESSION_CURRENT_START, Long.toString(System.currentTimeMillis()));
	} catch (IOException e) {
	  log.error("Unable to retrieve playback url for slug=" + item.get("slug"), e);
	}
      }

      currentIndex++;
      if (session.getAttributes().containsKey(Integer.toString(currentIndex))) {
	session.setAttribute(SESSION_CURRENT_INDEX, currentIndex);
      } else {
	session.setAttribute(SESSION_CURRENT_INDEX, null);
      }

      if (!directives.isEmpty()) {
	SsmlOutputSpeech output = new SsmlOutputSpeech();
	output.setSsml("<speak>" + speechOutput.toString() + "</speak>");

	SpeechletResponse response = SpeechletResponse.newTellResponse(output);
	response.setDirectives(directives);
	response.setShouldEndSession(false);

	return response;
      }
    }

    String repromptText =
      "<speak>I'm sorry I didn't understand that. You can say things like," +
      "teach me about java <break time=\"0.2s\" /> " +
      "help me with excel <break time=\"0.2s\" /> " +
      "courses <break time=\"0.2s\" /> " +
      "videos <break time=\"0.2s\" /> " +
      "learning paths. Or you can say exit. Now, what can I help you with?</speak>";
    return newAskResponse("What else would you like to learn about?", false, repromptText, true);
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
	    "You can ask about content. For example, teach me about Java, or get popular courses, or you can say exit."
                        + "Now, what can I help you with?";
        String repromptText =
                "I'm sorry I didn't understand that. You can say things like," +
                "teach me about java <break time=\"0.2s\" /> " +
                "help me with excel <break time=\"0.2s\" /> " +
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
