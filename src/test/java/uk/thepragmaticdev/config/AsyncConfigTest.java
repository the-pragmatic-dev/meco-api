package uk.thepragmaticdev.config;

// import static org.hamcrest.MatcherAssert.assertThat;
// import static org.hamcrest.Matchers.is;

import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootTest
class AsyncConfigTest {

  // @Autowired
  // private AsyncConfig sut;

  @BeforeEach
  public void initEach() {
  }

  @Test()
  void shouldReturnValidExecutor() {
    // var executor = (ThreadPoolTaskExecutor) sut.getAsyncExecutor();
    // assertThat(executor.getClass(), is(ThreadPoolTaskExecutor.class));
  }

  @Test()
  void shouldReturnCustomErrorHandler() throws InterruptedException, ExecutionException {

  }

}