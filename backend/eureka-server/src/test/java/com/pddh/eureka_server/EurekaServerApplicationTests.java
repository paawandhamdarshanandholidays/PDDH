package com.pddh.eureka_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EurekaServerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void constructor_shouldInstantiateApplicationClass() {
		EurekaServerApplication app = new EurekaServerApplication();

		assertThat(app).isNotNull();
	}

	@Test
	void main_shouldStartSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			springApplication.when(() -> SpringApplication.run(EurekaServerApplication.class, new String[0]))
					.thenReturn(null);

			EurekaServerApplication.main(new String[0]);

			springApplication.verify(() -> SpringApplication.run(EurekaServerApplication.class, new String[0]));
		}
	}

}
