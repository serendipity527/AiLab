package com.example.ailab.AiAgents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.agent.MissingArgumentException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.*;

import java.util.List;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.ArrayList;
import java.util.List;

public class AsyncAgentExample {
    private static final String API_KEY = "sk-43070f4cd1074965a93a03d6d5333cd8";
    public static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    // ========== 1. 定义专家代理接口 ==========
    public interface CreativeWriter {

        @UserMessage("""
            You are a creative writer.
            Generate a draft of a story no more than
            3 sentences long around the given topic.
            Return only the story and nothing else.
            The topic is {{topic}}.
            """)
        @Agent("Generates a story based on the given topic")
        String generateStory(@V("topic") String topic);
    }
    public interface AudienceEditor {

        @UserMessage("""
        You are a professional editor.
        Analyze and rewrite the following story to better align
        with the target audience of {{audience}}.
        Return only the story and nothing else.
        The story is "{{story}}".
        """)
        @Agent("Edits a story to better fit a given audience")
        String editStory(@V("story") String story, @V("audience") String audience);
    }
    public interface StyleEditor {

        @UserMessage("""
        You are a professional editor.
        Analyze and rewrite the following story to better fit and be more coherent with the {{style}} style.
        Return only the story and nothing else.
        The story is "{{story}}".
        """)
        @Agent("Edits a story to better fit a given style")
        String editStory(@V("story") String story, @V("style") String style);
    }
    public interface FoodExpert {
        @UserMessage("""
            You are a great evening planner.
            Propose a list of 3 meals matching the given mood.
            The mood is {{mood}}.
            For each meal, just give the name of the meal.
            Provide a list with the 3 items and nothing else.
            """)
        @Agent
        List<String> findMeal(@V("mood") String mood);
    }

    public interface MovieExpert {
        @UserMessage("""
            You are a great evening planner.
            Propose a list of 3 movies matching the given mood.
            The mood is {{mood}}.
            Provide a list with the 3 items and nothing else.
            """)
        @Agent
        List<String> findMovie(@V("mood") String mood);
    }

    // ========== 2. 定义结果数据类 ==========

    public record EveningPlan(String movie, String meal) {}

    // ========== 3. 定义协调器接口 ==========

    public interface EveningPlannerAgent {
        List<EveningPlan> plan(@V("mood") String mood);
    }

    // ========== 4. 主程序 ==========
    // 异步
    public static void anysnTest(){
        // 创建 ChatModel（根据你的实际情况配置）
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(API_KEY)
                .baseUrl(BASE_URL)
                .modelName("qwen-flash")
                .build();

        // 创建异步的 FoodExpert 代理
        FoodExpert foodExpert = AgenticServices.agentBuilder(FoodExpert.class)
                .chatModel(chatModel)
                .async(true)  // 🔥 设置为异步
                .outputKey("meals")  // 结果存储到 scope 的 "meals" 键
                .build();

        // 创建异步的 MovieExpert 代理
        MovieExpert movieExpert = AgenticServices.agentBuilder(MovieExpert.class)
                .chatModel(chatModel)
                .async(true)  // 🔥 设置为异步
                .outputKey("movies")  // 结果存储到 scope 的 "movies" 键
                .build();

        // 创建顺序工作流（虽然是顺序，但子代理是异步的）
        EveningPlannerAgent planner = AgenticServices.sequenceBuilder(EveningPlannerAgent.class)
                .subAgents(foodExpert, movieExpert)
                .outputKey("plans")
                .output(agenticScope -> {
                    // 从 scope 读取异步执行的结果
                    List<String> movies = agenticScope.readState("movies", List.of());
                    List<String> meals = agenticScope.readState("meals", List.of());

                    // 组合结果
                    List<EveningPlan> plans = new ArrayList<>();
                    for (int i = 0; i < Math.min(movies.size(), meals.size()); i++) {
                        plans.add(new EveningPlan(movies.get(i), meals.get(i)));
                    }
                    return plans;
                })
                .build();

        // 执行
        System.out.println("Planning a romantic evening...");
        List<EveningPlan> plans = planner.plan("romantic");

        // 输出结果
        plans.forEach(plan ->
                System.out.println("Movie: " + plan.movie() + " | Meal: " + plan.meal())
        );
    }

    public interface MedicalExpertWithMemory {

        @UserMessage("""
        You are a medical expert.
        Analyze the following user request under a medical point of view and provide the best possible answer.
        The user request is {{request}}.
        """)
        @Agent("A medical expert")
        String medical(@MemoryId String memoryId, @V("request") String request);
    }
    public interface ExpertRouterAgentWithMemory {

        @Agent
        String ask(@MemoryId String memoryId, @V("request") String request);
    }
    public interface ContextSummarizer {

        @UserMessage("""
        Create a very short summary, 2 sentences at most, of the
        following conversation between an AI agent and a user.

        The user conversation is: '{{it}}'.
        """)
        String summarize(String conversation);
    }

    public static void main(String[] args) {

    }
}