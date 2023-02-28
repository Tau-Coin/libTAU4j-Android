package io.taucbd.news.publishing.core.meetup;

public class OpenAIImpl implements AIInterface {

    private static final String API_KEY = "api-key";

    public OpenAIImpl() {}

    @Override
    public String getCompletion(String prompt) {
        return new String("default-prompt");
    }
}
