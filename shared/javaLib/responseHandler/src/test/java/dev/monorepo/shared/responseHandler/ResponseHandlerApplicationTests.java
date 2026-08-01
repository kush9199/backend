package dev.monorepo.shared.responseHandler;

import dev.monorepo.shared.responseHandler.common.ApiResponse;
import dev.monorepo.shared.responseHandler.common.AppException;
import dev.monorepo.shared.responseHandler.common.ErrorPayload;
import dev.monorepo.shared.responseHandler.config.SharedResponseAutoConfig;
import dev.monorepo.shared.responseHandler.config.YamlErrorLoader;
import dev.monorepo.shared.responseHandler.error.ErrorCatalog;
import dev.monorepo.shared.responseHandler.exception.SharedExceptionHandler;
import dev.monorepo.shared.responseHandler.filter.TraceIdFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResponseHandlerApplicationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SharedResponseAutoConfig.class));

	@Nested
	class ErrorCatalogTests {

		@Test
		void resolve_knownCode_returnsPayload() {
			Map<String, ErrorPayload> entries = Map.of(
					"USER_NOT_FOUND", new ErrorPayload("USER_NOT_FOUND", "not found", 404, "CLIENT_ERROR"),
					"INTERNAL_ERROR", new ErrorPayload("INTERNAL_ERROR", "oops", 500, "SERVER_ERROR")
			);
			ErrorCatalog catalog = new ErrorCatalog(entries);

			ErrorPayload result = catalog.resolve("USER_NOT_FOUND");

			assertEquals("USER_NOT_FOUND", result.code());
			assertEquals(404, result.httpStatus());
		}

		@Test
		void resolve_unknownCode_fallsBackToInternalError() {
			Map<String, ErrorPayload> entries = Map.of(
					"INTERNAL_ERROR", new ErrorPayload("INTERNAL_ERROR", "oops", 500, "SERVER_ERROR")
			);
			ErrorCatalog catalog = new ErrorCatalog(entries);

			ErrorPayload result = catalog.resolve("SOME_MADE_UP_CODE");

			assertEquals("INTERNAL_ERROR", result.code());
		}
	}

	@Nested
	class YamlErrorLoaderTests {

		@Test
		void load_validYaml_parsesAllFields() throws IOException {
			Map<String, ErrorPayload> result = YamlErrorLoader.load(
					"classpath:errors/default-errors.yaml", new DefaultResourceLoader());

			assertTrue(result.containsKey("INTERNAL_ERROR"));
			ErrorPayload p = result.get("INTERNAL_ERROR");
			assertEquals(500, p.httpStatus());
			assertEquals("SERVER_ERROR", p.category());
		}

		@Test
		void load_nullPath_returnsEmptyMap() throws IOException {
			assertTrue(YamlErrorLoader.load(null, new DefaultResourceLoader()).isEmpty());
		}

		@Test
		void load_nonExistentFile_returnsEmptyMap() throws IOException {
			assertTrue(YamlErrorLoader.load(
					"classpath:does-not-exist.yaml", new DefaultResourceLoader()).isEmpty());
		}

		@Test
		void load_malformedYaml_throwsIllegalStateException() {
			assertThrows(IllegalStateException.class, () ->
					YamlErrorLoader.load("classpath:errors/malformed-errors.yaml", new DefaultResourceLoader()));
		}
	}

	@Nested
	class ApiResponseTests {

		@AfterEach
		void clearMdc() { MDC.clear(); }

		@Test
		void success_wrapsDataCorrectly() {
			ApiResponse<String> response = ApiResponse.success("hello");

			assertTrue(response.isSuccess());
			assertEquals("hello", response.getData());
			assertNull(response.getError());
			assertNotNull(response.getTimestamp());
		}

		@Test
		void success_picksUpTraceIdFromMdc() {
			MDC.put("traceId", "trace-abc-123");
			ApiResponse<String> response = ApiResponse.success("data");
			assertEquals("trace-abc-123", response.getTraceId());
		}

		@Test
		void error_wrapsPayloadCorrectly() {
			ErrorPayload payload = new ErrorPayload("VALIDATION_FAILED", "bad input", 400, "CLIENT_ERROR");
			ApiResponse<?> response = ApiResponse.error(payload);

			assertFalse(response.isSuccess());
			assertNull(response.getData());
			assertEquals("VALIDATION_FAILED", response.getError().code());
		}

		@Test
		void response_noMdcSet_traceIdIsNull() {
			assertNull(ApiResponse.success("data").getTraceId());
		}
	}

	@Nested
	class TraceIdFilterTests {

		@Test
		void existingHeader_isReused() throws Exception {
			TraceIdFilter filter = new TraceIdFilter();
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addHeader("X-Trace-Id", "incoming-trace-999");
			MockHttpServletResponse response = new MockHttpServletResponse();
			FilterChain chain = mock(FilterChain.class);

			filter.doFilter(request, response, chain);

			assertEquals("incoming-trace-999", response.getHeader("X-Trace-Id"));
		}

		@Test
		void missingHeader_generatesNewTraceId() throws Exception {
			TraceIdFilter filter = new TraceIdFilter();
			MockHttpServletRequest request = new MockHttpServletRequest();
			MockHttpServletResponse response = new MockHttpServletResponse();
			FilterChain chain = mock(FilterChain.class);

			filter.doFilter(request, response, chain);

			assertNotNull(response.getHeader("X-Trace-Id"));
			assertFalse(response.getHeader("X-Trace-Id").isBlank());
		}

		@Test
		void mdcCleared_afterFilterCompletes() throws Exception {
			TraceIdFilter filter = new TraceIdFilter();
			MockHttpServletRequest request = new MockHttpServletRequest();
			MockHttpServletResponse response = new MockHttpServletResponse();
			FilterChain chain = mock(FilterChain.class);

			filter.doFilter(request, response, chain);

			assertNull(MDC.get("traceId"));
		}

		@Test
		void mdcCleared_evenWhenChainThrows() throws Exception {
			TraceIdFilter filter = new TraceIdFilter();
			MockHttpServletRequest request = new MockHttpServletRequest();
			MockHttpServletResponse response = new MockHttpServletResponse();
			FilterChain chain = mock(FilterChain.class);
			doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

			assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, chain));
			assertNull(MDC.get("traceId"));
		}
	}


	@Nested
	class SharedExceptionHandlerTests {

		@Test
		void handleApp_knownCode_returnsCorrectStatusAndBody() {
			ErrorCatalog catalog = new ErrorCatalog(Map.of(
					"USER_NOT_FOUND", new ErrorPayload("USER_NOT_FOUND", "not found", 404, "CLIENT_ERROR"),
					"INTERNAL_ERROR", new ErrorPayload("INTERNAL_ERROR", "oops", 500, "SERVER_ERROR")
			));
			var handler = new SharedExceptionHandler(catalog);

			ResponseEntity<ApiResponse<?>> response = handler.handleApp(new AppException("USER_NOT_FOUND"));

			assertEquals(404, response.getStatusCode().value());
			assertFalse(response.getBody().isSuccess());
			assertEquals("USER_NOT_FOUND", response.getBody().getError().code());
		}

//		@Test
//		void handleUnknown_genericException_returns500() {
//			ErrorCatalog catalog = new ErrorCatalog(Map.of(
//					"INTERNAL_ERROR", new ErrorPayload("INTERNAL_ERROR", "oops", 500, "SERVER_ERROR")
//			));
//			SharedExceptionHandler handler = new SharedExceptionHandler(catalog);
//
//			ResponseEntity<ApiResponse<?>> response = handler.handleUnknown(new RuntimeException("crash"));
//
//			assertEquals(500, response.getStatusCode().value());
//			assertEquals("INTERNAL_ERROR", response.getBody().getError().code());
//		}
	}

	@Nested
	class SharedResponseAutoConfigurationTests {

		@Test
		void defaultCatalog_loadsWithoutAppOverride() {
			contextRunner.run(context -> {
				ErrorCatalog catalog = context.getBean(ErrorCatalog.class);
				assertThat(catalog.resolve("INTERNAL_ERROR").httpStatus()).isEqualTo(500);
			});
		}

		@Test
		void appOverride_mergesOnTopOfDefaults() {
			contextRunner
					.withPropertyValues("shared.responseHandler.error-config-path=classpath:errors/app-errors.yaml")
					.run(context -> {
						ErrorCatalog catalog = context.getBean(ErrorCatalog.class);
						assertThat(catalog.resolve("CUSTOM_APP_ERROR").httpStatus()).isEqualTo(422);
						assertThat(catalog.resolve("INTERNAL_ERROR").httpStatus()).isEqualTo(500);
					});
		}
	}
}
