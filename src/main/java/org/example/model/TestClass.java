package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.annotation.Property;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class TestClass {

    @Property(name = "stringProperty")
    private String property;

    private int intProperty;

    private Integer integerProperty;

    private Instant timeProperty;
}
