package org.simplejavamail.api.mailer.config;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.simplejavamail.internal.util.MiscUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;

/**
 * Config holder for PKCS12 store+key info used for S/MIME encrypting / decrypting.
 */
@EqualsAndHashCode
public final class Pkcs12Config {

	@NotNull private final byte[] pkcs12StoreData;
	@NotNull private final char[] storePassword;
	@NotNull private final String keyAlias;
	@NotNull private final char[] keyPassword;

	private Pkcs12Config(byte@NotNull[] pkcs12StoreData, @NotNull char[] storePassword, @NotNull String keyAlias, @NotNull char[] keyPassword) {
		this.pkcs12StoreData = pkcs12StoreData.clone();
		this.storePassword = storePassword;
		this.keyAlias = keyAlias;
		this.keyPassword = keyPassword;
	}
	@NotNull
	public static Pkcs12ConfigBuilder builder() {
		return new Pkcs12ConfigBuilder();
	}

	@NotNull
	public  byte[] getPkcs12StoreData() {
		return this.pkcs12StoreData.clone();
	}

	@NotNull
	public char[] getStorePassword() {
		return this.storePassword.clone();
	}

	@NotNull
	public String getKeyAlias() {
		return this.keyAlias;
	}

	@NotNull
	public char[] getKeyPassword() {
		return this.keyPassword.clone();
	}

	@Override
	public String toString() {
		@SuppressWarnings("StringBufferReplaceableByString")
		final StringBuilder sb = new StringBuilder("Pkcs12Config{")
				.append("  storePassword=***")
				.append(", keyAlias='").append(keyAlias).append('\'')
				.append(", keyPassword=***")
				.append('}');
		return sb.toString();
	}

	public static class Pkcs12ConfigBuilder {
		private byte[] pkcs12StoreData;
		private char[] storePassword;
		private String keyAlias;
		private char[] keyPassword;

		private Pkcs12ConfigBuilder() {
		}

		public Pkcs12ConfigBuilder pkcs12Store(String pkcs12StorePath) {
			return pkcs12Store(new File(pkcs12StorePath));
		}

		/**
		 * Note that this method creates a new {@code FileInputStream} without closing it.
		 */
		public Pkcs12ConfigBuilder pkcs12Store(File pkcs12StorePath) {
			try {
				return pkcs12Store(new FileInputStream(pkcs12StorePath));
			} catch (IOException e) {
				throw new IllegalStateException(format("error reading PKCS12 store from File [%s]", pkcs12StorePath), e);
			}
		}

		public Pkcs12ConfigBuilder pkcs12Store(InputStream pkcs12StoreStream)
				throws IOException {
			this.pkcs12StoreData = MiscUtil.readInputStreamToBytes(pkcs12StoreStream);
			return this;
		}

		public Pkcs12ConfigBuilder pkcs12Store(byte[] pkcs12StoreData) {
			this.pkcs12StoreData = pkcs12StoreData.clone();
			return this;
		}

		public Pkcs12ConfigBuilder storePassword(char[] storePassword) {
			this.storePassword = storePassword.clone();
			return this;
		}

		public Pkcs12ConfigBuilder storePassword(String storePassword) {
			this.storePassword = storePassword.toCharArray();
			return this;
		}

		public Pkcs12ConfigBuilder keyAlias(String keyAlias) {
			this.keyAlias = keyAlias;
			return this;
		}

		public Pkcs12ConfigBuilder keyPassword(char[] keyPassword) {
			this.keyPassword = keyPassword.clone();
			return this;
		}

		public Pkcs12ConfigBuilder keyPassword(String keyPassword) {
			this.keyPassword = keyPassword.toCharArray();
			return this;
		}

		public Pkcs12Config build() {
			return new Pkcs12Config(pkcs12StoreData, storePassword, keyAlias, keyPassword);
		}
	}
}