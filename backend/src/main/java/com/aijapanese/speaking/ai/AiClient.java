package com.aijapanese.speaking.ai;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.audio.speech.SpeechModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Spring AI로의 접근을 격리하는 공용 AI Client.
 * Prompt 내용이나 Business 규칙은 이 클래스에 두지 않고, Domain AI Service가 담당한다.
 */
@Component
public class AiClient {

    private final ChatModel chatModel;
    private final OpenAiAudioTranscriptionModel audioTranscriptionModel;
    private final SpeechModel speechModel;

    public AiClient(ChatModel chatModel, OpenAiAudioTranscriptionModel audioTranscriptionModel, SpeechModel speechModel) {
        this.chatModel = chatModel;
        this.audioTranscriptionModel = audioTranscriptionModel;
        this.speechModel = speechModel;
    }

    public String chat(Prompt prompt) {
        try {
            return chatModel.call(prompt).getResult().getOutput().getText();
        } catch (RuntimeException e) {
            throw new AiServiceException("AI 응답 생성 중 오류가 발생했습니다.", e);
        }
    }

    public String transcribe(Resource audioResource) {
        try {
            AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource);
            return audioTranscriptionModel.call(prompt).getResult().getOutput();
        } catch (RuntimeException e) {
            throw new AiServiceException("음성 인식 중 오류가 발생했습니다.", e);
        }
    }

    public byte[] synthesizeSpeech(String text) {
        try {
            SpeechPrompt prompt = new SpeechPrompt(text);
            return speechModel.call(prompt).getResult().getOutput();
        } catch (RuntimeException e) {
            throw new AiServiceException("음성 합성 중 오류가 발생했습니다.", e);
        }
    }
}
