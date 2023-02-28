package io.taucbd.news.publishing.core.meetup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.CompletionChoice;

import java.util.List;

public class OpenAIImpl implements AIInterface {

    private static final String TAG = MeetupManager.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);

    private static final String API_KEY = "api-key";

    private OpenAiService mService;

    public OpenAIImpl() {
        mService = new OpenAiService(API_KEY);
    }

    @Override
    public String getCompletion(String prompt) {
        CompletionRequest completionRequest = CompletionRequest.builder()
                .model("text-davinci-003")
                .prompt(prompt)
                .temperature(1.0)
                .topP(0.0)
                .n(1)
                .build();
        List<CompletionChoice> choices
                = mService.createCompletion(completionRequest).getChoices();

        if (choices.size() > 0) {
            return choices.get(0).getText();
        }

        return new String("");
    }
}
