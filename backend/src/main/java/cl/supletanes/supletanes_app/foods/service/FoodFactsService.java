package cl.supletanes.supletanes_app.foods.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import cl.supletanes.supletanes_app.foods.dto.FoodDTO;
import cl.supletanes.supletanes_app.foods.dto.OpenFoodResponse;
import cl.supletanes.supletanes_app.foods.dto.OpenFoodSearchResponse;

@Service
public class FoodFactsService {

    private RestTemplate restTemplate;

    public FoodFactsService() {
        this.restTemplate = new RestTemplate();
    }

    public FoodFactsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public FoodDTO getProductByBarcode(String barcode) {
        if (barcode == null || !barcode.matches("^[0-9A-Za-z_-]{1,32}$")) {
            throw new IllegalArgumentException("Código de barras inválido");
        }

        String encodedBarcode = URLEncoder.encode(barcode, StandardCharsets.UTF_8);
        String url = "https://world.openfoodfacts.org/api/v2/product/" + encodedBarcode + ".json";

        OpenFoodResponse response = restTemplate.getForObject(url, OpenFoodResponse.class);
        if (response == null || response.getProduct() == null) {
            FoodDTO emptyDto = new FoodDTO();
            emptyDto.setCalorias(0.0);
            return emptyDto;
        }

        return mapToFoodDTO(response.getProduct());
    }

    public List<FoodDTO> searchProductsByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nombre de alimento inválido");
        }

        String encodedName = URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        String url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms="
                + encodedName
                + "&search_simple=1&action=process&json=1";

        restTemplate.getForObject(url, String.class);
        OpenFoodSearchResponse response = restTemplate.getForObject(url, OpenFoodSearchResponse.class);
        if (response == null || response.getProducts() == null || response.getProducts().isEmpty()) {
            return Collections.emptyList();
        }

        return response.getProducts().stream()
                .map(this::mapToFoodDTO)
                .filter(Objects::nonNull)
                .toList();
    }

    private FoodDTO mapToFoodDTO(OpenFoodResponse.Product p) {
        if (p == null) {
            return null;
        }

        OpenFoodResponse.Nutriments n = p.getNutriments();

        Double calorias = 0.0;
        Double proteinas = 0.0;
        Double carbohidratos = 0.0;
        Double grasas = 0.0;

        if (n != null) {
            if (n.getEnergy_kcal_100g() != null && n.getEnergy_kcal_100g() != 0.0) {
                calorias = n.getEnergy_kcal_100g();
            } else if (n.getEnergy_kcal_serving() != null && n.getEnergy_kcal_serving() != 0.0) {
                calorias = n.getEnergy_kcal_serving();
            } else if (n.getEnergy_100g() != null) {
                calorias = n.getEnergy_100g();
            }

            if (n.getProteins_100g() != null) {
                proteinas = n.getProteins_100g();
            } else if (n.getProteins_serving() != null) {
                proteinas = n.getProteins_serving();
            }

            if (n.getCarbohydrates_100g() != null) {
                carbohidratos = n.getCarbohydrates_100g();
            } else if (n.getCarbohydrates_serving() != null) {
                carbohidratos = n.getCarbohydrates_serving();
            }

            if (n.getFat_100g() != null) {
                grasas = n.getFat_100g();
            } else if (n.getFat_serving() != null) {
                grasas = n.getFat_serving();
            }
        }

        FoodDTO dto = new FoodDTO();
        dto.setNombre(p.getProduct_name());
        dto.setCalorias(calorias);
        dto.setProteinas(proteinas);
        dto.setCarbohidratos(carbohidratos);
        dto.setGrasas(grasas);
        return dto;
    }
}