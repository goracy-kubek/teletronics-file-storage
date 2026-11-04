package com.teletronics.storage.mapper;

import com.teletronics.storage.domain.FileReservation;
import com.teletronics.storage.service.command.UploadCommand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileReservationMapper {
    FileReservation toReservation(UploadCommand uploadCommand);
}