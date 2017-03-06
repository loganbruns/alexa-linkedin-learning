package linkedinlearning.storage;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshaller;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshalling;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@DynamoDBTable(tableName = "LinkedInLearningUserData")
public class LearningUserDataItem {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private String customerId;

  private LearningUserData userData;

  @DynamoDBHashKey(attributeName = "CustomerId")
  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  @DynamoDBAttribute(attributeName = "Data")
  @DynamoDBMarshalling(marshallerClass = LearningUserDataMarshaller.class)
  public LearningUserData getUserData() {
    return userData;
  }

  public void setUserData(LearningUserData userData) {
    this.userData = userData;
  }

  /**
   * A {@link DynamoDBMarshaller} that provides marshalling and unmarshalling logic for
   * {@link LearningUserData} values so that they can be persisted in the database as String.
   */
  public static class LearningUserDataMarshaller implements
						   DynamoDBMarshaller<LearningUserData> {

    @Override
      public String marshall(LearningUserData userData) {
      try {
	return OBJECT_MAPPER.writeValueAsString(userData);
      } catch (JsonProcessingException e) {
	throw new IllegalStateException("Unable to marshall user data", e);
      }
    }

    @Override
      public LearningUserData unmarshall(Class<LearningUserData> clazz, String value) {
      try {
	return OBJECT_MAPPER.readValue(value, new TypeReference<LearningUserData>() {
	  });
      } catch (Exception e) {
	throw new IllegalStateException("Unable to unmarshall user data value", e);
      }
    }
  }
}
