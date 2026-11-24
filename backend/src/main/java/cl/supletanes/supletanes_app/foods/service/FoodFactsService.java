package cl.supletanes.supletanes_app.foods.service;

import cl.supletanes.supletanes_app.foods.dto.FoodDTO;
import cl.supletanes.supletanes_app.foods.dto.OpenFoodResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
public class FoodFactsService {

    private final RestTemplate restTemplate;

    public FoodFactsService() {
        this.restTemplate = new RestTemplate();
    }

    public FoodFactsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public FoodDTO getProductByBarcode(String barcode) {
        if (barcode == null || !barcode.matches("^[0-9A-Za-z_-]{4,32}$")) {
            throw new IllegalArgumentException("Código de barras inválido");
        }

        String encodedBarcode = URLEncoder.encode(barcode, StandardCharsets.UTF_8);
        String url = "https://world.openfoodfacts.org/api/v2/product/" + encodedBarcode + ".json";

        OpenFoodResponse response = restTemplate.getForObject(url, OpenFoodResponse.class);
        if (response == null || response.getProduct() == null) {
            return null;
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

        OpenFoodResponse response = restTemplate.getForObject(url, OpenFoodResponse.class);
        if (response == null || response.getProduct() == null) {
            return Collections.emptyList();
        }

        FoodDTO dto = mapToFoodDTO(response.getProduct());
        return dto != null ? List.of(dto) : Collections.emptyList();
    }

    private FoodDTO mapToFoodDTO(OpenFoodResponse.Product p) {
        if (p == null) {
            return null;
        }

        OpenFoodResponse.Nutriments n = p.getNutriments();

        Double calorias = n != null ? n.getEnergy_kcal_100g() : null;
        if ((calorias == null || calorias == 0.0) && n != null) {
            calorias = n.getEnergy_100g();
        }

        FoodDTO dto = new FoodDTO();
        dto.setNombre(p.getProduct_name());
        dto.setCalorias(calorias);
        if (n != null) {
            dto.setProteinas(n.getProteins_100g());
            dto.setCarbohidratos(n.getCarbohydrates_100g());
            dto.setGrasas(n.getFat_100g());
        }
        return dto;
    }
}