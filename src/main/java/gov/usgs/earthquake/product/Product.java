/*
 * Product
 */
package gov.usgs.earthquake.product;

import gov.usgs.earthquake.distribution.ProductKey;
import gov.usgs.util.CryptoUtils;
import gov.usgs.util.XmlUtils;

import java.security.PublicKey;
import java.security.PrivateKey;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;

import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One or more pieces of Content with metadata.
 *
 * <dl>
 * <dt><strong>ID</strong></dt>
 * <dd>
 * Products each have a unique {@link ProductId}.
 * </dd>
 *
 * <dt><strong>Versioning</strong></dt>
 * <dd>
 * It is possible to create multiple versions of the same product,
 * by reusing the same <code>source</code>, <code>type</code>, and
 * <code>code</code>, with a different <code>updateTime</code>.
 * <br>
 * More recent (newer) <code>updateTime</code>s <strong>supersede</strong>
 * Less recent (older) <code>updateTime</code>s.
 * </dd>
 *
 * <dt><strong>Status</strong></dt>
 * <dd>
 * To <strong>delete</strong> a product, create a new version (updateTime)
 * and set it's status to {@link STATUS_DELETE}.  All other statuses
 * ({@link STATUS_UPDATE} by default) are considered updates, and any
 * value can be used in product-specific ways.
 * </dd>
 *
 * <dt><strong>Properties</strong></dt>
 * <dd>
 * Products have key/value attributes that are Strings.
 * These can be useful to convey summary information about a product,
 * so consumers can quickly decide whether to process before opening
 * any product contents.
 * </dd>
 *
 * <dt><strong>Links</strong></dt>
 * <dd>
 * Similar to properties, links allow a Product to specify a
 * <code>relation</code> and one or more <code>link</code> for each
 * relation type.
 * Links must be {@link java.net.URI}s, and may be {@link ProductId}s.
 * </dd>
 *
 * <dt><strong>Contents</strong></dt>
 * <dd>
 * Many Products start as a directory of files, and metadata is determined later.
 * It's also possible to create products without any Contents attached,
 * if all the necessary information can be encoded using Properties or Links.
 * <br>
 * One special "empty path" content, literally at the empty-string path,
 * is handled differently; since an empty path cannot be written to a file.
 * PDL typically reads this in from standard input, or delivers this on
 * standard input to external processes.
 * </dd>
 *
 * <dt><strong>Signature</strong></dt>
 * <dd>
 * A product can have a digital signature, based on a digest of all
 * product contents and metadata.  These are required for most purposes.
 * {@link CryptoUtils} provides utilities for working with OpenSSH keypairs.
 * </dd>
 *
 * <dt><strong>Tracker URL (Deprecated)</strong></dt>
 * <dd>
 * Tracker URLs were initially used to track processing status as
 * distribution progressed.  These are no longer supported, and often
 * introduced new problems.
 * </dd>
 * </dl>
 */
public class Product {

	private static final Logger LOGGER = Logger.getLogger(Product.class
			.getName());

	/** The status message when a product is being updated. */
	public static final String STATUS_UPDATE = "UPDATE";

	/** The status message when a product is being deleted. */
	public static final String STATUS_DELETE = "DELETE";

	public static final String EVENTSOURCE_PROPERTY = "eventsource";
	public static final String EVENTSOURCECODE_PROPERTY = "eventsourcecode";
	public static final String EVENTTIME_PROPERTY = "eventtime";
	public static final String MAGNITUDE_PROPERTY = "magnitude";
	public static final String LATITUDE_PROPERTY = "latitude";
	public static final String LONGITUDE_PROPERTY = "longitude";
	public static final String DEPTH_PROPERTY = "depth";
	public static final String VERSION_PROPERTY = "version";

	/** A unique identifier for this product. */
	private ProductId id;

	/** A terse status message. */
	private String status;

	/** Properties of this product. */
	private Map<String, String> properties = new HashMap<String, String>();

	/** Links to other products and related resources. */
	private Map<String, List<URI>> links = new HashMap<String, List<URI>>();

	/** Product contents. Mapping from path to content. */
	private Map<String, Content> contents = new HashMap<String, Content>();

	/** A URL where status updates are sent. */
	private URL trackerURL = null;

	/** A signature generated by the product creator. */
	private String signature = null;

	/**
	 * Construct a new Product with status "UPDATE".
	 * 
	 * @param id
	 *            the product's unique Id.
	 */
	public Product(final ProductId id) {
		this(id, STATUS_UPDATE);
	}

	/**
	 * Construct a new Product.
	 * 
	 * @param id
	 *            the product's unique Id.
	 * @param status
	 *            the product's status.
	 */
	public Product(final ProductId id, final String status) {
		setId(id);
		setStatus(status);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param that
	 *            the product to copy.
	 */
	public Product(final Product that) {
		this(new ProductId(that.getId().getSource(), that.getId().getType(),
				that.getId().getCode(), that.getId().getUpdateTime()), that
				.getStatus());
		this.setTrackerURL(that.getTrackerURL());
		this.setProperties(that.getProperties());
		this.setLinks(that.getLinks());
		this.setContents(that.getContents());
		this.setSignature(that.getSignature());
	}

	/**
	 * @return the id
	 */
	public ProductId getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(final ProductId id) {
		this.id = id;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(final String status) {
		this.status = status;
	}

	/**
	 * Product.STATUS_DELETE.equalsIgnoreCase(status).
	 * 
	 * @return whether this product is deleted
	 */
	public boolean isDeleted() {
		if (STATUS_DELETE.equalsIgnoreCase(this.status)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return the properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(final Map<String, String> properties) {
		this.properties.putAll(properties);
	}

	/**
	 * Returns a reference to the links map.
	 * 
	 * @return the links
	 */
	public Map<String, List<URI>> getLinks() {
		return links;
	}

	/**
	 * Copies entries from provided map.
	 * 
	 * @param links
	 *            the links to set
	 */
	public void setLinks(final Map<String, List<URI>> links) {
		this.links.putAll(links);
	}

	/**
	 * Add a link to a product.
	 * 
	 * @param relation
	 *            how link is related to product.
	 * @param href
	 *            actual link.
	 */
	public void addLink(final String relation, final URI href) {
		List<URI> relationLinks = links.get(relation);
		if (relationLinks == null) {
			relationLinks = new LinkedList<URI>();
			links.put(relation, relationLinks);
		}
		relationLinks.add(href);
	}

	/**
	 * Returns a reference to the contents map.
	 * 
	 * @return the contents
	 */
	public Map<String, Content> getContents() {
		return contents;
	}

	/**
	 * Copies entries from provided map.
	 * 
	 * @param contents
	 *            the contents to set
	 */
	public void setContents(final Map<String, Content> contents) {
		this.contents.clear();
		this.contents.putAll(contents);
	}

	/**
	 * @return the trackerURL
	 */
	public URL getTrackerURL() {
		return trackerURL;
	}

	/**
	 * @param trackerURL
	 *            the trackerURL to set
	 */
	public void setTrackerURL(final URL trackerURL) {
		this.trackerURL = trackerURL;
	}

	/**
	 * @return the signature
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * @param signature
	 *            the signature to set
	 */
	public void setSignature(final String signature) {
		this.signature = signature;
	}

	/**
	 * Sign this product using a PrivateKey.
	 * 
	 * @param privateKey
	 *            a DSAPrivateKey or RSAPrivateKey.
	 * @throws Exception
	 */
	public void sign(final PrivateKey privateKey) throws Exception {
		setSignature(CryptoUtils.sign(privateKey,
				ProductDigest.digestProduct(this)));
	}

	/**
	 * Verify this product's signature.
	 * 
	 * When a product has no signature, this method returns false. The array of
	 * public keys corresponds to one or more keys that may have generated the
	 * signature. If any of the keys verify, this method returns true.
	 * 
	 * @param publicKeys
	 *            an array of publicKeys to test.
	 * @return true if valid, false otherwise.
	 * @throws Exception
	 */
	public boolean verifySignature(final PublicKey[] publicKeys)
			throws Exception {
		return verifySignatureKey(publicKeys) != null;
	}

	public PublicKey verifySignatureKey(final PublicKey[] publicKeys) throws Exception {
		if (signature == null) {
			return null;
		}

		byte[] digest = ProductDigest.digestProduct(this);
		for (PublicKey key : publicKeys) {
			try {
				if (CryptoUtils.verify(key, digest, getSignature())) {
					return key;
				}
			} catch (Exception e) {
				LOGGER.log(Level.FINEST, "Exception while verifying signature",
								e);
			}
		}
		return null;
	}

	/**
	 * Get the event id.
	 * 
	 * The event id is the combination of event source and event source code.
	 * 
	 * @return the event id, or null if either event source or event source code
	 *         is null.
	 */
	public String getEventId() {
		String eventSource = getEventSource();
		String eventSourceCode = getEventSourceCode();
		if (eventSource == null && eventSourceCode == null) {
			return null;
		}
		return (eventSource + eventSourceCode).toLowerCase();
	}

	/**
	 * Set both the network and networkId at the same time.
	 * 
	 * @param source
	 *            the originating network.
	 * @param sourceCode
	 *            the originating network's id.
	 */
	public void setEventId(final String source, final String sourceCode) {
		setEventSource(source);
		setEventSourceCode(sourceCode);
	}

	/**
	 * Get the event source property.
	 * 
	 * @return the event source property, or null if no event source property
	 *         set.
	 */
	public String getEventSource() {
		return this.properties.get(EVENTSOURCE_PROPERTY);
	}

	/**
	 * Set the event source property.
	 * 
	 * @param eventSource
	 *            the event source to set.
	 */
	public void setEventSource(final String eventSource) {
		if (eventSource == null) {
			this.properties.remove(EVENTSOURCE_PROPERTY);
		} else {
			this.properties
					.put(EVENTSOURCE_PROPERTY, eventSource.toLowerCase());
		}
	}

	/**
	 * Get the event source code property.
	 * 
	 * @return the event source code property, or null if no event source code
	 *         property set.
	 */
	public String getEventSourceCode() {
		return this.properties.get(EVENTSOURCECODE_PROPERTY);
	}

	/**
	 * Set the event id property.
	 * 
	 * @param eventSourceCode
	 *            the event id to set.
	 */
	public void setEventSourceCode(final String eventSourceCode) {
		if (eventSourceCode == null) {
			this.properties.remove(EVENTSOURCECODE_PROPERTY);
		} else {
			this.properties.put(EVENTSOURCECODE_PROPERTY,
					eventSourceCode.toLowerCase());
		}
	}

	/**
	 * Get the event time property as a date.
	 * 
	 * @return the event time property as a date, or null if no date property
	 *         set.
	 */
	public Date getEventTime() {
		String strDate = this.properties.get(EVENTTIME_PROPERTY);
		if (strDate == null) {
			return null;
		}
		return XmlUtils.getDate(strDate);
	}

	/**
	 * Set the event time property as a date.
	 * 
	 * @param eventTime
	 *            the event time to set.
	 */
	public void setEventTime(final Date eventTime) {
		if (eventTime == null) {
			this.properties.remove(EVENTTIME_PROPERTY);
		} else {
			this.properties.put(EVENTTIME_PROPERTY,
					XmlUtils.formatDate(eventTime));
		}
	}

	/**
	 * Get the magnitude property as a big decimal.
	 * 
	 * @return the magnitude property as a big decimal, or null if no magnitude
	 *         property set.
	 */
	public BigDecimal getMagnitude() {
		String strMag = this.properties.get(MAGNITUDE_PROPERTY);
		if (strMag == null) {
			return null;
		}
		return new BigDecimal(strMag);
	}

	/**
	 * Set the magnitude property as a big decimal.
	 * 
	 * @param magnitude
	 *            the magnitude to set.
	 */
	public void setMagnitude(final BigDecimal magnitude) {
		if (magnitude == null) {
			this.properties.remove(MAGNITUDE_PROPERTY);
		} else {
			this.properties.put(MAGNITUDE_PROPERTY, magnitude.toPlainString());
		}
	}

	/**
	 * Get the latitude property as a big decimal.
	 * 
	 * @return latitude property as a big decimal, or null if no latitude
	 *         property set.
	 */
	public BigDecimal getLatitude() {
		String strLat = this.properties.get(LATITUDE_PROPERTY);
		if (strLat == null) {
			return null;
		}
		return new BigDecimal(strLat);
	}

	/**
	 * Set the latitude property as a big decimal.
	 * 
	 * @param latitude
	 *            the latitude to set.
	 */
	public void setLatitude(final BigDecimal latitude) {
		if (latitude == null) {
			this.properties.remove(LATITUDE_PROPERTY);
		} else {
			this.properties.put(LATITUDE_PROPERTY, latitude.toPlainString());
		}
	}

	/**
	 * Get the longitude property as a big decimal.
	 * 
	 * @return longitude property as a big decimal, or null if no longitude
	 *         property set.
	 */
	public BigDecimal getLongitude() {
		String strLon = this.properties.get(LONGITUDE_PROPERTY);
		if (strLon == null) {
			return null;
		}
		return new BigDecimal(strLon);
	}

	/**
	 * Set the longitude property as a big decimal.
	 * 
	 * @param longitude
	 *            the longitude to set.
	 */
	public void setLongitude(final BigDecimal longitude) {
		if (longitude == null) {
			this.properties.remove(LONGITUDE_PROPERTY);
		} else {
			this.properties.put(LONGITUDE_PROPERTY, longitude.toPlainString());
		}
	}

	/**
	 * Get the depth property as a big decimal.
	 * 
	 * @return depth property as big decimal, or null if no depth property set.
	 */
	public BigDecimal getDepth() {
		String strDepth = this.properties.get(DEPTH_PROPERTY);
		if (strDepth == null) {
			return null;
		}
		return new BigDecimal(strDepth);
	}

	/**
	 * Set the depth property as a big decimal.
	 * 
	 * @param depth
	 *            the depth to set.
	 */
	public void setDepth(final BigDecimal depth) {
		if (depth == null) {
			this.properties.remove(DEPTH_PROPERTY);
		} else {
			this.properties.put(DEPTH_PROPERTY, depth.toPlainString());
		}
	}

	/**
	 * Get the version property.
	 * 
	 * @return the version property, or null if no version property set.
	 */
	public String getVersion() {
		return this.properties.get(VERSION_PROPERTY);
	}

	/**
	 * Set the version property.
	 * 
	 * @param version
	 *            the version to set.
	 */
	public void setVersion(final String version) {
		if (version == null) {
			this.properties.remove(VERSION_PROPERTY);
		} else {
			this.properties.put(VERSION_PROPERTY, version);
		}
	}

}
