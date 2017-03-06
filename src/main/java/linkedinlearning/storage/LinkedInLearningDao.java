package linkedinlearning.storage;

import com.amazon.speech.speechlet.Session;

public class LinkedInLearningDao {
  private final LinkedInLearningDbClient _dbClient;

  public LinkedInLearningDao(LinkedInLearningDbClient dbClient) {
    _dbClient = dbClient;
  }

  public LearningUserData getUserData(Session session) {
    LearningUserDataItem item = new LearningUserDataItem();
    item.setCustomerId(session.getUser().getUserId());

    item = _dbClient.load(item);
    
    if (item == null) {
      return new LearningUserData();
    }

    return item.getUserData();
  }

  public void saveUserData(Session session, LearningUserData userData) {
    LearningUserDataItem item = new LearningUserDataItem();
    item.setCustomerId(session.getUser().getUserId());
    item.setUserData(userData);

    _dbClient.save(item);
  }
}
