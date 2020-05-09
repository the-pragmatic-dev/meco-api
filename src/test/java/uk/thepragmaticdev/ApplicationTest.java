package uk.thepragmaticdev;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.security.request.RequestMetadataService;

@ExtendWith(MockitoExtension.class)
public class ApplicationTest {

  @Mock
  private RequestMetadataService requestMetadataService;

  private Application sut;

  @BeforeEach
  public void initEach() {
    sut = new Application(requestMetadataService);
  }

  @Test
  public void shouldThrowExceptionIfDatabaseFailsToLoad() {
    when(requestMetadataService.loadDatabase()).thenReturn(false);
    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.init();
    });
    assertThat(ex.getErrorCode(), is(CriticalCode.GEOLITE_DOWNLOAD_ERROR));
  }
}