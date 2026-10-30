package rw.farm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.farm.common.PageResponse;
import rw.farm.common.Pages;
import rw.farm.domain.Animal;
import rw.farm.domain.AnimalEvent;
import rw.farm.domain.AnimalWeightRecord;
import rw.farm.domain.Farm;
import rw.farm.dto.OperationDtos.CreateEventRequest;
import rw.farm.dto.OperationDtos.CreateWeightRequest;
import rw.farm.dto.OperationDtos.EventResponse;
import rw.farm.dto.OperationDtos.WeightResponse;
import rw.farm.repo.AnimalEventRepository;
import rw.farm.repo.AnimalWeightRecordRepository;

import java.time.Instant;
import java.util.UUID;

import static rw.farm.service.AccessService.WEIGHT_WRITE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackingService {

    private final AnimalWeightRecordRepository weights;
    private final AnimalEventRepository events;
    private final AnimalService animals;
    private final AccessService access;
    private final DomainSupport support;

    public PageResponse<WeightResponse> weights(UUID animalId, int page, int size) {
        animals.get(animalId);
        return PageResponse.of(weights.findByAnimalIdOrderByMeasuredAtDesc(animalId, Pages.of(page, size)).map(this::toResponse));
    }

    @Transactional
    public WeightResponse recordWeight(UUID animalId, CreateWeightRequest request) {
        Farm farm = access.requireFarmWrite(request.farmId(), WEIGHT_WRITE);
        Animal animal = animals.requirePresent(animalId, farm.getId());
        AnimalWeightRecord weight = new AnimalWeightRecord();
        weight.setAnimal(animal);
        weight.setFarm(farm);
        weight.setMeasuredAt(request.measuredAt() == null ? Instant.now() : request.measuredAt());
        weight.setWeightKg(request.weightKg());
        weight.setMeasurementMethod(DomainSupport.trimToNull(request.measurementMethod()));
        weight.setNotes(DomainSupport.trimToNull(request.notes()));
        weight.setRecordedBy(support.actor());
        return toResponse(weights.save(weight));
    }

    public PageResponse<EventResponse> events(UUID animalId, int page, int size) {
        animals.get(animalId);
        return PageResponse.of(events.findByAnimalIdOrderByEventAtDesc(animalId, Pages.of(page, size)).map(this::toResponse));
    }

    @Transactional
    public EventResponse recordEvent(UUID animalId, CreateEventRequest request) {
        Animal animal = animals.readable(animalId);
        Farm farm = request.farmId() == null ? null : access.requireFarmWrite(request.farmId(), WEIGHT_WRITE);
        if (farm == null) {
            access.requireAnyRole(WEIGHT_WRITE);
        }
        AnimalEvent event = new AnimalEvent();
        event.setAnimal(animal);
        event.setFarm(farm);
        event.setEventType(request.eventType());
        event.setEventAt(request.eventAt() == null ? Instant.now() : request.eventAt());
        event.setDetails(DomainSupport.trimToNull(request.details()));
        event.setRecordedBy(support.actor());
        return toResponse(events.save(event));
    }

    private WeightResponse toResponse(AnimalWeightRecord weight) {
        return new WeightResponse(
                weight.getId(), weight.getAnimal().getId(), weight.getFarm().getId(), weight.getMeasuredAt(),
                weight.getWeightKg(), weight.getMeasurementMethod(), weight.getNotes(), weight.getCreatedAt());
    }

    private EventResponse toResponse(AnimalEvent event) {
        return new EventResponse(
                event.getId(),
                event.getAnimal().getId(),
                event.getFarm() == null ? null : event.getFarm().getId(),
                event.getEventType(),
                event.getEventAt(),
                event.getDetails(),
                event.getCreatedAt());
    }
}
