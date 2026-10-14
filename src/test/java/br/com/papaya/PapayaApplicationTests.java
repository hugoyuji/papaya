package br.com.papaya;

import br.com.papaya.integration.PapayaPostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PapayaPostgresTestConfiguration.class)
class PapayaApplicationTests {

	@Test
	void contextLoads() {
	}

}
