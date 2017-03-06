package linkedinlearning.storage;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

public class LinkedInLearningDbClient {
  private final AmazonDynamoDBClient _dbClient;

  public LinkedInLearningDbClient(final AmazonDynamoDBClient dynamoDBClient) {
    this._dbClient = dynamoDBClient;
  }

  public LearningUserDataItem load(final LearningUserDataItem dataItem) {
    return (new DynamoDBMapper(_dbClient)).load(dataItem);
  }

  public void save(final LearningUserDataItem dataItem) {
    (new DynamoDBMapper(_dbClient)).save(dataItem);
  }
}
