package com.cookiek.commenthat.autoProcessor;

import com.cookiek.commenthat.autoProcessor.service.FetchChannelInfoService;
import com.cookiek.commenthat.domain.User;
import com.cookiek.commenthat.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Rollback(value = false)
//@Rollback
public class AutoProcessorTest {

    @Autowired
    FetchChannelInfoService fetchChannelInfoService;
    @Autowired UserService userService;
    @Autowired EntityManager em;


    @Test
    public void testFetchChannelInfo() throws Exception {
        //given
        Long userId = 2L;

        //when
        User user = userService.findUserById(userId);
        String channelId = user.getChannelId();
        fetchChannelInfoService.fetchAndSaveAsync(channelId, user.getId());

        // Then
        Thread.sleep(3000); // 간단 대기 (임시, 비동기 작업 끝날 때까지 대기)

        //http://localhost:8080/fetch-channel-info?userId=1
    }

}

