package com.example.youthcon.handson;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.example.youthcon.preparation.*;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class CommentService {

    private HashMap<String, Set<SseEmitter>> container = new HashMap<>();

    public SseEmitter connect(final String articleId) {
        // 새로운 이미터 생성
        //  - 타임아웃이 길면, 서버에서 관리해야 할 OverHead 증가
        //  - 타임아웃이 짧으면, 재연결 요청이 증가
        final SseEmitter sseEmitter = new SseEmitter(5 * 60 * 1000L ); //5분으로 생성

        // 전송할 이벤트를 작성
        final SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event()
                .name("connect")
                .data("connected!!")
                .reconnectTime(3000L);

        // 작성한 이벤트를 생성한 이미터에 전송
        sendEvent(sseEmitter, sseEventBuilder);

        // 아티클과 연결된 이미터 컨테이너를 생성
        final Set<SseEmitter> sseEmitters = container.getOrDefault(articleId, new HashSet<>());
        sseEmitters.add(sseEmitter);
        container.put(articleId, sseEmitters);

        return sseEmitter;
    }

    private void sendEvent(final SseEmitter sseEmitter, final SseEmitter.SseEventBuilder sseEventBuilder) {
        try {
            sseEmitter.send(sseEventBuilder.build());
        } catch (IOException e) {
            //서버에서는 클라이언트의 상태를 알 수 없기 때문에, 만약 클라이언트가 Tab을 닫은 상태라면, IO Exception이 발생함
            throw new RuntimeException(e);
        }
    }

}
