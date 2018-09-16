/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.Stack;

import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.platform.engine.TestDescriptor;

/**
 * Test correct test discovery in simple test classes for {@link DisplayNameGeneration}.
 *
 * @since 5.4
 */
class DisplayNameGenerationTests extends AbstractJupiterTestEngineTests {

	@Test
	void defaultStyle() {
		check(DefaultStyleTestCase.class, //
			"CONTAINER: DisplayNameGenerationTests$DefaultStyleTestCase", //
			"TEST: @DisplayName prevails", //
			"TEST: test()", //
			"TEST: test(TestInfo)", //
			"TEST: testUsingCamelCaseStyle()", //
			"TEST: testUsingCamelCase_and_also_UnderScores()", //
			"TEST: test_with_underscores()" //
		);
	}

	@Test
	void underscoreStyle() {
		var expectedDisplayNames = new String[] { //
				"CONTAINER: DisplayNameGenerationTests\\$UnderscoreStyle.*", //
				"TEST: @DisplayName prevails", //
				"TEST: test with underscores()", //
				"TEST: test()", //
				"TEST: test(TestInfo)", //
				"TEST: testUsingCamelCase and also UnderScores()", //
				"TEST: testUsingCamelCaseStyle()" };
		check(UnderscoreStyleTestCase.class, expectedDisplayNames);
		check(UnderscoreStyleInheritedFromSuperClassTestCase.class, expectedDisplayNames);
	}

	@Test
	void noNameStyle() {
		check(NoNameStyleTestCase.class, //
			"CONTAINER: nn", //
			"TEST: @DisplayName prevails", //
			"TEST: nn", //
			"TEST: nn", //
			"TEST: nn", //
			"TEST: nn", //
			"TEST: nn" //
		);
	}

	@Test
	void checkDisplayNameGeneratedForTestingAStackDemo() {
		check(StackTestCase.class, //
			"CONTAINER: A new stack", //
			"CONTAINER: A stack", //
			"CONTAINER: After pushing an element to an empty stack", //
			"TEST: is empty()", //
			"TEST: is instantiated using its noarg constructor()", //
			"TEST: peek returns that element without removing it from the stack()", //
			"TEST: pop returns that element and leaves an empty stack()", //
			"TEST: the stack is no longer empty()", //
			"TEST: throws an EmptyStackException when peeked()", //
			"TEST: throws an EmptyStackException when popped()" //
		);
	}

	private void check(Class<?> testClass, String... expectedDisplayNames) {
		var request = request().selectors(selectClass(testClass)).build();
		var descriptors = discoverTests(request).getDescendants();
		var sortedNames = descriptors.stream().map(this::describe).sorted().collect(toList());
		assertLinesMatch(Arrays.asList(expectedDisplayNames), sortedNames);
	}

	private String describe(TestDescriptor descriptor) {
		return descriptor.getType() + ": " + descriptor.getDisplayName();
	}

	// -------------------------------------------------------------------

	static class NoNameGenerator implements DisplayNameGenerator {

		@Override
		public String generateDisplayNameForClass(Class<?> testClass) {
			return "nn";
		}

		@Override
		public String generateDisplayNameForNestedClass(Class<?> nestedClass) {
			return "nn";
		}

		@Override
		public String generateDisplayNameForMethod(Class<?> testClass, Method testMethod) {
			return "nn";
		}
	}

	@DisplayNameGeneration(generator = NoNameGenerator.class)
	static abstract class AbstractTestCase {
		@Test
		void test() {
		}

		@Test
		void test(TestInfo testInfo) {
			testInfo.getDisplayName();
		}

		@Test
		void testUsingCamelCaseStyle() {
		}

		@Test
		void testUsingCamelCase_and_also_UnderScores() {
		}

		@Test
		void test_with_underscores() {
		}

		@DisplayName("@DisplayName prevails")
		@Test
		void testDisplayNamePrevails() {
		}
	}

	@DisplayNameGeneration(DisplayNameGeneration.Style.DEFAULT)
	static class DefaultStyleTestCase extends AbstractTestCase {
	}

	@DisplayNameGeneration(DisplayNameGeneration.Style.UNDERSCORE)
	static class UnderscoreStyleTestCase extends AbstractTestCase {
	}

	@DisplayNameGeneration(generator = NoNameGenerator.class)
	static class NoNameStyleTestCase extends AbstractTestCase {
	}

	// No annotation here! @DisplayNameGeneration is inherited from super class
	static class UnderscoreStyleInheritedFromSuperClassTestCase extends UnderscoreStyleTestCase {
	}

	// -------------------------------------------------------------------

	@DisplayName("A stack")
	@DisplayNameGeneration(DisplayNameGeneration.Style.UNDERSCORE)
	static class StackTestCase {

		Stack<Object> stack;

		@Test
		void is_instantiated_using_its_noarg_constructor() {
			new Stack<>();
		}

		@Nested
		class A_new_stack {

			@BeforeEach
			void createNewStack() {
				stack = new Stack<>();
			}

			@Test
			void is_empty() {
				assertTrue(stack.isEmpty());
			}

			@Test
			void throws_an_EmptyStackException_when_popped() {
				assertThrows(EmptyStackException.class, () -> stack.pop());
			}

			@Test
			void throws_an_EmptyStackException_when_peeked() {
				assertThrows(EmptyStackException.class, () -> stack.peek());
			}

			@Nested
			class After_pushing_an_element_to_an_empty_stack {

				String anElement = "an element";

				@BeforeEach
				void pushAnElement() {
					stack.push(anElement);
				}

				@Test
				void the_stack_is_no_longer_empty() {
					assertFalse(stack.isEmpty());
				}

				@Test
				void pop_returns_that_element_and_leaves_an_empty_stack() {
					assertEquals(anElement, stack.pop());
					assertTrue(stack.isEmpty());
				}

				@Test
				void peek_returns_that_element_without_removing_it_from_the_stack() {
					assertEquals(anElement, stack.peek());
					assertFalse(stack.isEmpty());
				}
			}
		}
	}
}
