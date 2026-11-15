package jetpackcodesolution.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import jetpackcodesolution.domain.exception.InvalidPageRequestException;
import jetpackcodesolution.domain.model.BookingSearchCriteria;
import jetpackcodesolution.domain.model.BookingSearchResult;
import jetpackcodesolution.domain.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchBookingsServiceTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Mock
    private BookingRepository bookingRepository;

    private SearchBookingsService service;

    @BeforeEach
    void setUp() {
        service = new SearchBookingsService(bookingRepository, UTC);
    }

    @Test
    void convertsDateToHalfOpenDayBoundsInUtc() {
        when(bookingRepository.search(any())).thenReturn(new BookingSearchResult(List.of(), 0));

        service.searchBookings(7L, LocalDate.parse("2026-12-01"), false, 1, 50);

        ArgumentCaptor<BookingSearchCriteria> captor =
                ArgumentCaptor.forClass(BookingSearchCriteria.class);
        verify(bookingRepository).search(captor.capture());
        BookingSearchCriteria criteria = captor.getValue();
        assertThat(criteria.roomId()).isEqualTo(7L);
        assertThat(criteria.dayStart()).isEqualTo(Instant.parse("2026-12-01T00:00:00Z"));
        assertThat(criteria.dayEnd()).isEqualTo(Instant.parse("2026-12-02T00:00:00Z"));
        assertThat(criteria.includeCancelled()).isFalse();
        assertThat(criteria.page()).isEqualTo(1);
        assertThat(criteria.size()).isEqualTo(50);
    }

    @Test
    void usesConfiguredZoneForDayBounds() {
        SearchBookingsService caracas =
                new SearchBookingsService(bookingRepository, ZoneId.of("America/Caracas"));
        when(bookingRepository.search(any())).thenReturn(new BookingSearchResult(List.of(), 0));

        caracas.searchBookings(null, LocalDate.parse("2026-12-01"), false, 0, 20);

        ArgumentCaptor<BookingSearchCriteria> captor =
                ArgumentCaptor.forClass(BookingSearchCriteria.class);
        verify(bookingRepository).search(captor.capture());
        assertThat(captor.getValue().dayStart()).isEqualTo(Instant.parse("2026-12-01T04:00:00Z"));
        assertThat(captor.getValue().dayEnd()).isEqualTo(Instant.parse("2026-12-02T04:00:00Z"));
    }

    @Test
    void leavesDayBoundsNullWhenDateIsNotProvided() {
        when(bookingRepository.search(any())).thenReturn(new BookingSearchResult(List.of(), 0));

        service.searchBookings(null, null, true, 0, 20);

        ArgumentCaptor<BookingSearchCriteria> captor =
                ArgumentCaptor.forClass(BookingSearchCriteria.class);
        verify(bookingRepository).search(captor.capture());
        assertThat(captor.getValue().dayStart()).isNull();
        assertThat(captor.getValue().dayEnd()).isNull();
        assertThat(captor.getValue().includeCancelled()).isTrue();
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> service.searchBookings(null, null, false, -1, 20))
                .isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("El parámetro 'page' no puede ser negativo.");

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void rejectsSizeBelowOne() {
        assertThatThrownBy(() -> service.searchBookings(null, null, false, 0, 0))
                .isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("El parámetro 'size' debe estar entre 1 y 100.");

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void rejectsSizeAboveMaximum() {
        assertThatThrownBy(() -> service.searchBookings(null, null, false, 0, 101))
                .isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("El parámetro 'size' debe estar entre 1 y 100.");

        verifyNoInteractions(bookingRepository);
    }
}
