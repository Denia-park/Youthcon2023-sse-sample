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
        final SseEmitter sseEmitter = new SseEmitter(5 * 60 * 1000L);
        //시간이 1초밖에 되지 않기때문에, 금방 만료가 되는데 container에서 만료된 SSE를 제거하지 않고 그대로 가지고 있기 때문에 forEach를 돌 때 예외가 발생한다.
        //그래서 만료됐을때 처리할 CallBack 함수를 추가해줘야 한다. // sseEmitter.onCompletion();

        // 전송할 이벤트를 작성
        final SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event()
                .name("connect")
                .data("connected!!")
                .reconnectTime(3 * 1000L);

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

    public void sendComment(final Comment comment, final String articleId) {
        //아티클과 연결된 이미터들을 모두 가져오기
        final Set<SseEmitter> sseEmitters = container.getOrDefault(articleId, new HashSet<>());

        //가져온 이미터들에게 댓글을 전송하기
        final SseEmitter.SseEventBuilder sseEventBuilder = SseEmitter.event()
                .name("newComment")
                .data(comment)
                .reconnectTime(3000L);

        sseEmitters.forEach(sseEmitter -> sendEvent(sseEmitter, sseEventBuilder));
    }
}
