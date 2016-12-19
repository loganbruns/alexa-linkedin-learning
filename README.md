# Unofficial LinkedIn Learning Alexa Skill

## Features (so far..)

### Current
This skill supports:

- Communicates with learning-api to find courses, videos, and learning paths.
- Dialog and Session State: Handles two models, both a one-shot ask and tell model, and a multi-turn dialog model.
  If the user provides an incorrect slot in a one-shot model, it will direct to the dialog model
- Uses pagination: Handles paginating a list of responses to avoid overwhelming the learner.
- Uses SSML: Using SSML tags to control how Alexa renders the text-to-speech.

### Future
Features to be considered for the future:

- Improve card formatting and include links to courses, videos, and learning paths
- "Teach me ..." to do key word searches for courses, videos, and learning paths
- Support launching learning app
- Support playing content
- Account binding (to be able to access user specific data and privileges.)

## Setup
Until this has more features and is published you have to deploy it as a lambda yourself and configure a private Alexa skill to use the Lambda.

### AWS Lambda Setup
1. Go to the AWS Console and click on the Lambda link. Note: ensure you are in us-east or you wont be able to use Alexa with Lambda.
2. Click on the Create a Lambda Function or Get Started Now button.
3. Skip the blueprint
4. Name the Lambda Function "LinkedIn-Learning-Skill".
5. Select the runtime as Java 8
6. Go to the the root directory containing pom.xml, and run 'mvn assembly:assembly -DdescriptorId=jar-with-dependencies package'. This will generate a zip file named "alexa-linkedin-learning-1.0-jar-with-dependencies.jar" in the target directory.
7. Select Code entry type as "Upload a .ZIP file" and then upload the "alexa-linkedin-learning-1.0-jar-with-dependencies.jar" file from the build directory to Lambda
8. Set the Handler as linkedinlearning.LinkedInLearningSpeechletRequestStreamHandler (this refers to the Lambda RequestStreamHandler file in the zip).
9. Create a basic execution role and click create.
10. Leave the Advanced settings as the defaults.
11. Click "Next" and review the settings then click "Create Function"
12. Click the "Event Sources" tab and select "Add event source"
13. Set the Event Source type as Alexa Skills kit and Enable it now. Click Submit.
14. Copy the ARN from the top right to be used later in the Alexa Skill Setup.

### Alexa Skill Setup
1. Go to the [Alexa Console](https://developer.amazon.com/edw/home.html) and click Add a New Skill.
2. Set "LinkedIn Learning" as the skill name and "linked in learning" as the invocation name, this is what is used to activate your skill. For example you would say: "Alexa, Ask LinkedIn Learning about popular courses."
3. Select the Lambda ARN for the skill Endpoint and paste the ARN copied from above. Click Next.
4. Copy the custom slot types from the customSlotTypes folder. Each file in the folder represents a new custom slot type. The name of the file is the name of the custom slot type, and the values in the file are the values for the custom slot.
5. Copy the Intent Schema from the included IntentSchema.json.
6. Copy the Sample Utterances from the included SampleUtterances.txt. Click Next.
7. Go back to the skill Information tab and copy the appId. Paste the appId into the LinkedInLearningSpeechletRequestStreamHandler.java file for the variable supportedApplicationIds,
   then update the lambda source zip file with this change and upload to lambda again, this step makes sure the lambda function only serves request from authorized source.
8. You are now able to start testing your sample skill! You should be able to go to the [Echo webpage](http://echo.amazon.com/#skills) and see your skill enabled.
9. In order to test it, try to say some of the Sample Utterances from the Examples section below.
10. Your skill is now saved and once you are finished testing you can continue to publish your skill.

## Examples
### One-shot model
     User:  "Alexa, ask LinkedIn Learning about popular courses"
     Alexa: "Here are the popular in courses. The most popular is .... Would you like
             to hear more?"
     User:  "No"

### Dialog model:
     User:  "Alexa, open LinkedIn Learning"
     Alexa: "Welcome to LinkedIn Learning. For which category do you want to hear the popular content?"
     User:  "courses"
     Alexa: "Here are the popular in courses. The most popular is .... Would you like
             to hear more?"
     User:  "yes"
     Alexa: "Second ... Third... Fourth... Would you like to hear more?"
     User : "no"
