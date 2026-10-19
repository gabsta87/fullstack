package com.serv.requests;

import java.util.List;

public record UpdateWorkerProfile(String username,
                                  String description,
                                  String shortDescription,
                                  Integer geographicZoneId,
                                  String eyeColor,
                                  String hairColor,
                                  String phone,
                                  String bodyType,
                                  String mainPhotoId,
                                  List<String> services,
                                  String birthdate) {
}
