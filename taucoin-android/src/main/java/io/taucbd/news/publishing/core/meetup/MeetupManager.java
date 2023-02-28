package io.taucbd.news.publishing.core.meetup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucbd.news.publishing.core.storage.RepositoryHelper;

public class MeetupManager {

    private static final String TAG = MeetupManager.class.getSimpleName();
    private static final Logger logger = LoggerFactory.getLogger(TAG);

    private static volatile MeetupManager sSingleton;

    private AIInterface mAI;
    // TODO: meetup and meetup_scheme table instance

    public static MeetupManager getInstance() {
        if (sSingleton == null) {
            synchronized (MeetupManager.class) {
                if (sSingleton == null)
                    sSingleton = new MeetupManager();
            }
        }
        
        return sSingleton;
    }

    private MeetupManager() {
        mAI = new OpenAIImpl();
        // TODO: get database instance
    }

    public boolean addMeetup(Meetup meetup) {
        return false;
    }

    public String getSchemeByPrompt(String prompt) {
        return mAI.getCompletion(prompt);
    }
}
