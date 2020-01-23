package gov.usgs.earthquake.origin;

import java.math.BigDecimal;
import java.util.Map;
import java.util.logging.Logger;

import gov.usgs.earthquake.geoserve.GeoservePlacesService;
import gov.usgs.earthquake.indexer.DefaultIndexerModule;
import gov.usgs.earthquake.indexer.IndexerModule;
import gov.usgs.earthquake.indexer.ProductSummary;
import gov.usgs.earthquake.product.Product;

public class OriginIndexerModule extends DefaultIndexerModule {
  private static final Logger LOGGER = Logger.getLogger(OriginIndexerModule.class.getName());

  private GeoservePlacesService geoservePlaces;

  public OriginIndexerModule() {
    this.geoservePlaces = new GeoservePlacesService();
  }

  @Override
  public int getSupportLevel(Product product) {
    int supportLevel = IndexerModule.LEVEL_UNSUPPORTED;
    String type = getBaseProductType(product.getId().getType());

    if ("origin".equals(type) && !"DELETE".equalsIgnoreCase(product.getStatus())) {
      supportLevel = IndexerModule.LEVEL_SUPPORTED;
    }

    return supportLevel;
  }

  // @Override
  public ProductSummary getProductSummary(Product product) throws Exception {
    ProductSummary summary = super.getProductSummary(product);
    BigDecimal latitude = summary.getEventLatitude();
    BigDecimal longitude = summary.getEventLongitude();

    // Defer to existing title property if set...
    Map<String, String> summaryProperties = summary.getProperties();
    String title = summaryProperties.get("title");

    if (title == null && latitude != null && longitude != null) {
      try {
        title = this.geoservePlaces.getEventTitle(latitude, longitude);
        summaryProperties.put("title", title);
      } catch (Exception ex) {
        LOGGER.finer(ex.getMessage());
        // Do nothing, value-added failed. Move on.
      }
    }

    return summary;
  }

}