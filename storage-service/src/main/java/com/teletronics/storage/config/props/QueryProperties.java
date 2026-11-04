package com.teletronics.storage.config.props;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Validated
@ConfigurationProperties("storage.query")
public record QueryProperties(@NotEmpty Set<String> sortableProperties) {
}