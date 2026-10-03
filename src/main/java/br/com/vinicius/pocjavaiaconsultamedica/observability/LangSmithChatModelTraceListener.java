package br.com.vinicius.pocjavaiaconsultamedica.observability;

import br.com.vinicius.pocjavaiaconsultamedica.config.LangSmithProperties;
import br.com.vinicius.pocjavaiaconsultamedica.config.OpenRouterProperties;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Observa as chamadas feitas pelo LangChain4j ao OpenRouter.
 *
 * O LangGraph4j já cria spans dos nodes/edges. Este listener cria um span específico
 * para cada chamada ao modelo. Como a chamada é síncrona, o span do modelo vira filho
 * do node que o disparou.
 *
 * No LangSmith isso permite enxergar algo parecido com:
 *
 * medical_appointments
 *   -> identifyIntent
 *       -> openrouter.chat
 *   -> schedule
 *   -> message
 *       -> openrouter.chat
 */
@Component
public class LangSmithChatModelTraceListener implements ChatModelListener {

    private static final Object SPAN_ATTRIBUTE_KEY = new Object();
    private static final Object SCOPE_ATTRIBUTE_KEY = new Object();

    private final Tracer tracer;
    private final LangSmithProperties langSmithProperties;
    private final OpenRouterProperties openRouterProperties;

    public LangSmithChatModelTraceListener(
            OpenTelemetry openTelemetry,
            LangSmithProperties langSmithProperties,
            OpenRouterProperties openRouterProperties
    ) {
        this.tracer = openTelemetry.getTracer("poc_java_ia_consultamedica.langchain4j");
        this.langSmithProperties = langSmithProperties;
        this.openRouterProperties = openRouterProperties;
    }

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        if (!langSmithProperties.isTracingEnabled()) {
            return;
        }

        Span span = tracer.spanBuilder("openrouter.chat")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("langsmith.trace.name", "OpenRouter Chat")
                .setAttribute("langsmith.span.kind", "llm")
                .setAttribute("gen_ai.operation.name", "chat")
                .setAttribute("gen_ai.system", "openrouter")
                .setAttribute("gen_ai.request.model", openRouterProperties.model())
                .setAttribute("langsmith.metadata.framework", "langchain4j")
                .setAttribute("langsmith.metadata.project_component", "medical-appointment-poc")
                .startSpan();

        if (langSmithProperties.includeLlmPayloads()) {
            /*
             * input.value é entendido pelo LangSmith como input do run.
             * A POC contém apenas dados fictícios; não envie dados médicos reais.
             */
            span.setAttribute(
                    "input.value",
                    requestContext.chatRequest().messages().toString()
            );
        }

        Scope scope = span.makeCurrent();

        Map<Object, Object> attributes = requestContext.attributes();
        attributes.put(SPAN_ATTRIBUTE_KEY, span);
        attributes.put(SCOPE_ATTRIBUTE_KEY, scope);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        if (!langSmithProperties.isTracingEnabled()) {
            return;
        }

        Span span = findSpan(responseContext.attributes());
        try {
            if (span == null) {
                return;
            }

            if (langSmithProperties.includeLlmPayloads()
                    && responseContext.chatResponse().aiMessage() != null
                    && responseContext.chatResponse().aiMessage().text() != null) {
                span.setAttribute(
                        "output.value",
                        responseContext.chatResponse().aiMessage().text()
                );
            }

            TokenUsage tokenUsage = responseContext
                    .chatResponse()
                    .metadata()
                    .tokenUsage();

            if (tokenUsage != null) {
                setTokenAttribute(span, "gen_ai.usage.input_tokens", tokenUsage.inputTokenCount());
                setTokenAttribute(span, "gen_ai.usage.output_tokens", tokenUsage.outputTokenCount());
                setTokenAttribute(span, "gen_ai.usage.total_tokens", tokenUsage.totalTokenCount());
            }

            if (responseContext.chatResponse().metadata().modelName() != null) {
                span.setAttribute(
                        "gen_ai.response.model",
                        responseContext.chatResponse().metadata().modelName()
                );
            }

            span.setStatus(StatusCode.OK);
        } finally {
            closeScopeAndEndSpan(responseContext.attributes(), span);
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        if (!langSmithProperties.isTracingEnabled()) {
            return;
        }

        Span span = findSpan(errorContext.attributes());
        try {
            if (span == null) {
                return;
            }

            if (errorContext.error() != null) {
                span.recordException(errorContext.error());
                span.setStatus(
                        StatusCode.ERROR,
                        errorContext.error().getMessage() == null
                                ? "OpenRouter call failed"
                                : errorContext.error().getMessage()
                );
            }
        } finally {
            closeScopeAndEndSpan(errorContext.attributes(), span);
        }
    }

    private Span findSpan(Map<Object, Object> attributes) {
        Object value = attributes.get(SPAN_ATTRIBUTE_KEY);
        return value instanceof Span span ? span : null;
    }

    private void closeScopeAndEndSpan(Map<Object, Object> attributes, Span span) {
        Object scopeObject = attributes.remove(SCOPE_ATTRIBUTE_KEY);
        attributes.remove(SPAN_ATTRIBUTE_KEY);

        if (scopeObject instanceof Scope scope) {
            scope.close();
        }

        if (span != null) {
            span.end();
        }
    }

    private void setTokenAttribute(Span span, String attributeName, Integer value) {
        if (value != null) {
            span.setAttribute(attributeName, value.longValue());
        }
    }
}
