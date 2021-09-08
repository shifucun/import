// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.datacommons.server;

import java.util.List;
import org.datacommons.proto.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableAutoConfiguration
@RestController
public class ServerApplication {
  // Dependency injection of ObservationRepository instance.
  @Autowired private ObservationRepository observationRepository;

  public static void main(String[] args) {
    SpringApplication.run(ServerApplication.class, args);
  }

  @GetMapping("/stat/set/series/all")
  public ResponseEntity<Api.GetStatSetSeriesResponse> getStatSeries(
      @RequestParam(value = "places") String[] places,
      @RequestParam(value = "statVars") String[] statVars) {
    if (places.length == 0 || statVars.length == 0) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
    // Init and build the reponse with empty values.
    Api.GetStatSetSeriesResponse.Builder resp = Api.GetStatSetSeriesResponse.newBuilder();
    for (String place : places) {
      resp.putData(place, Api.SeriesMap.getDefaultInstance());
      for (String statVar : statVars) {
        resp.getDataOrThrow(place).toBuilder().putData(statVar, Api.Series.getDefaultInstance());
      }
    }
    // Populate actual data from query results.
    List<Observation> observations =
        observationRepository.findObservationByPlaceAndStatVar(places, statVars);
    for (Observation observation : observations) {
      String place = observation.getObservationAbout();
      String date = observation.getObservationDate();
      String statVar = observation.getVariableMeasured();
      Double value = observation.getValue();
      resp.getDataOrThrow(place).getDataOrThrow(statVar).toBuilder().putVal(date, value);
    }
    return new ResponseEntity<Api.GetStatSetSeriesResponse>(resp.build(), HttpStatus.OK);
  }
}
