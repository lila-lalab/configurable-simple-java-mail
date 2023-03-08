package org.simplejavamail.api.email;

import jakarta.activation.DataSource;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.simplejavamail.api.email.config.DkimConfig;
import org.simplejavamail.api.internal.clisupport.model.Cli;
import org.simplejavamail.api.internal.clisupport.model.CliBuilderApiType;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.EmailGovernance;
import org.simplejavamail.api.mailer.config.Pkcs12Config;
import org.simplejavamail.internal.config.EmailProperty;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Fluent interface Builder for populating {@link Email} instances. An instance of this builder can only be obtained through one of the builder
 * starters on {@link EmailStartingBuilder}.
 * <strong>Note:</strong> To start creating a new Email, you use {@code EmailBuilder} directly instead.
 * <p>
 * <strong>Note:</strong> For some reason, JavaDoc is not able to parse all {@code @link} directives used in this class' documentation. I have no idea why, if you can figure
 * it out, please let me know!
 */
@Cli.BuilderApiNode(builderApiType = CliBuilderApiType.EMAIL)
public interface EmailPopulatingBuilder {

	/**
	 * Regular Expression to find all {@code <img src="...">} entries in an HTML document.It needs to cater for various things, like more whitespaces including newlines on any
	 * place, HTML is not case-sensitive and there can be arbitrary text between "IMG" and "SRC" like IDs and other things.
	 */
	Pattern IMG_SRC_PATTERN = compile("(?<imageTagStart><[Ii][Mm][Gg]\\s*[^>]*?\\s+[Ss][Rr][Cc]\\s*=\\s*[\"'])(?<src>[^\"']+?)(?<imageSrcEnd>[\"'])");

	/**
	 * Validated DKIM values and then delegates to {@link Email#Email(EmailPopulatingBuilder)} with <code>this</code> as argument. This results in an Email instance with
	 * just the values set on this builder by the user. <strong>This is the regular use case and the common way to send emails using a {@link Mailer} instance.</strong>
	 * <p>
	 * If you don't have a Mailer instance or you just want to call helper methods that only accept an {@link EmailWithDefaultsAndOverridesApplied}, there are two ways
	 * to complete this Email with defaults and overrides that you may have configured as (system) properties (files):
	 * <ol>
	 *     <li>Construct a new {@code EmailGovernanceImpl} or use {@code EmailGovernanceImpl.NO_GOVERNANCE()}, and then use {@link EmailGovernance#produceEmailApplyingDefaultsAndOverrides(Email)}</li>
	 *     <li>Don't use {@link #buildEmail()}, but use {@link #buildEmailCompletedWithDefaultsAndOverrides()} or {@link #buildEmailCompletedWithDefaultsAndOverrides(EmailGovernance)} instead</li>
	 * </ol>
	 * It depends on whether you like fine-grained control over email governance (validation, max email size, defaults, overrides, etc.) or not.
	 *
	 * @see #buildEmailCompletedWithDefaultsAndOverrides(EmailGovernance)
	 */
	@SuppressWarnings("JavadocDeclaration")
	@Cli.ExcludeApi(reason = "This API is specifically for Java use")
	Email buildEmail();

	/**
	 * Delegates to {@link #buildEmailCompletedWithDefaultsAndOverrides(EmailGovernance)} with an empty default email governance, which
	 * will still apply default config (System) properties (files).
	 */
	@Cli.ExcludeApi(reason = "This API is specifically for Java use")
	Email buildEmailCompletedWithDefaultsAndOverrides();

	/**
	 * Like {@link #buildEmail()}, but returning the final email version right away. Useful if you don't use a Mailer which works with email governance,
	 * or just want to call a helper method that only accepts {@link EmailWithDefaultsAndOverridesApplied}. For regular cases of just sending with a {@link Mailer}, this
	 * completion stage happens automatically when converting to MimeMessage. In that case use {@link #buildEmail()} instead.
	 * <p>
	 * Why not always apply defaults and overrides? Because it would be a waste of resources to do so, especially when you are sending multiple emails resuing a mailer instance.
	 * Another use case is that when using a server cluster with the batch-module (multiple mailer instances), defaults set during sending ensure that the defaults set for a
	 * specific mailer are used. This is sometimes important if an SMTP server needs a specific sender address, or if you want to use a specific DKIM signature bound to that server.
	 *
	 * @param emailGovernance The email governance to use for this email. It determines which default values and overrides to apply, what the maximum
	 *                           email size is, etc.
	 */
	@Cli.ExcludeApi(reason = "This API is specifically for Java use")
	Email buildEmailCompletedWithDefaultsAndOverrides(@NotNull EmailGovernance emailGovernance);

	/**
	 * Indicates that when the email is sent, no default values whatsoever should be applied to the email.
	 *
	 * @param ignoreDefaults Whether to ignore all default values or not for this email.
	 *
	 * @see #dontApplyDefaultValueFor(EmailProperty...)
	 * @see org.simplejavamail.api.mailer.MailerRegularBuilder#withEmailDefaults(Email)
	 */
	@Cli.OptionNameOverride("ignoringDefaultsYesNo")
	EmailPopulatingBuilder ignoringDefaults(boolean ignoreDefaults);

	/**
	 * Indicates that when the email is sent, no override values whatsoever should be applied to the email.
	 *
	 * @param ignoreOverrides Whether to ignore all overrides values or not for this email.
	 *
	 * @see #dontApplyOverrideValueFor(EmailProperty...)
	 * @see org.simplejavamail.api.mailer.MailerRegularBuilder#withEmailOverrides(Email)
	 */
	@Cli.OptionNameOverride("ignoringOverridesYesNo")
	EmailPopulatingBuilder ignoringOverrides(boolean ignoreOverrides);

	/**
	 * Allows you to prevent a property to be configured with default values. This might be useful if you have defined defaults (either through (system) properties,
	 * config files, or on mailer level on the {@link org.simplejavamail.api.mailer.config.EmailGovernance}), but for a specific case or under certain conditions
	 * you might want to have this email untouched by the defaults.
	 * <br>
	 * <strong>Note:</strong> This is irrelevant for Email instances used to set on {@link org.simplejavamail.api.mailer.config.EmailGovernance}
	 * as defaults or overrides reference.
	 *
	 * @param configProperties The properties that should not be configured with default values, if any, when sending the email.
	 * @see EmailStartingBuilder#ignoringDefaults()
	 */
	EmailPopulatingBuilder dontApplyDefaultValueFor(@NotNull EmailProperty @NotNull ...configProperties);

	/**
	 * Allows you to prevent a property to be configured with override values. This might be useful if you have defined overrides on mailer level on the
	 * {@link org.simplejavamail.api.mailer.config.EmailGovernance}), but for a specific case or under certain conditions you might want
	 * to have this email untouched by the overrides.
	 *
	 * @param configProperties The properties that should not be overridden when sending the email.
	 */
	EmailPopulatingBuilder dontApplyOverrideValueFor(@NotNull EmailProperty @NotNull ...configProperties);
	
	/**
	 * Sets optional ID to a fixed value, which is otherwise generated by the underlying JavaMail framework when sending the email.
	 * <p>
	 * <strong>Note 1:</strong> ID is user-controlled. Only when converting an email, Simple Java Mail might fill the sent-date.
	 * <br>
	 * <strong>Note 2:</strong> The id-format should conform to <a href="https://tools.ietf.org/html/rfc5322#section-3.6.4">rfc5322#section-3.6.4</a>
	 *
	 * @param id The mime message id, example: {@code <123@456>}
	 */
	EmailPopulatingBuilder fixingMessageId(@Nullable String id);
	
	/**
	 * Delegates to {@link #from(String, String)} with empty name.
	 *
	 * @param fromAddress The sender address visible to receivers of the email.
	 */
	@Cli.ExcludeApi(reason = "API is subset of another API")
	EmailPopulatingBuilder from(@NotNull String fromAddress);
	
	/**
	 * Delegates to {@link #from(Recipient)} with a new {@link Recipient} wrapped around the given name and email address.
	 *
	 * @param name The name that will be visible to the receivers of this email.
	 * @param fromAddress The address that will be visible to the receivers of this email.
	 */
	EmailPopulatingBuilder from(@Nullable String name, @NotNull String fromAddress);
	
	/**
	 * Delegates to {@link #from(Recipient)} with a new {@link Recipient} wrapped around the given fixed name and email address.
	 */
	EmailPopulatingBuilder from(@Nullable String fixedName, @NotNull InternetAddress fromAddress);
	
	/**
	 * Delegates to {@link #from(Recipient)} with a new {@link Recipient} wrapped around the given email address.
	 */
	EmailPopulatingBuilder from(@NotNull InternetAddress fromAddress);
	
	/**
	 * Sets the address of the sender of this email with given {@link Recipient} (ignoring its {@link Message.RecipientType} if provided).
	 * <p>
	 * Can be used in conjunction with one of the {@code replyTo(...)} methods, which is then prioritized by email clients when replying to this
	 * email.
	 *
	 * @param recipient Preconfigured recipient which includes optional name and mandatory email address.
	 *
	 * @see #from(String, String)
	 * @see #from(String)
	 * @see #withReplyTo(Recipient)
	 */
	EmailPopulatingBuilder from(@NotNull Recipient recipient);
	
	/**
	 * Delegates to {@link #withReplyTo(Recipient)} with a  new {@link Recipient} wrapped around the given email address (or null if missing).
	 *
	 * @param replyToAddress The address that receivers will get when they reply to the email.
	 */
	@Cli.ExcludeApi(reason = "API is subset of another API")
	EmailPopulatingBuilder withReplyTo(@Nullable String replyToAddress);
	
	/**
	 * Delegates to {@link #withReplyTo(Recipient)} with a new {@link Recipient} wrapped around the given fixed name and email address.
	 *
	 * @param fixedName Optional name that receivers will get when they reply to the email.
	 * @param replyToAddress The address that receivers will get when they reply to the email. Any name included in the address will be ignored.
	 */
	EmailPopulatingBuilder withReplyTo(@Nullable String fixedName, @NotNull String replyToAddress);
	
	/**
	 * Delegates to {@link #withReplyTo(Recipient)} with a  new {@link Recipient} wrapped around the given address.
	 */
	EmailPopulatingBuilder withReplyTo(@NotNull InternetAddress replyToAddress);
	
	/**
	 * Delegates to {@link #withReplyTo(Recipient)} with a new {@link Recipient} wrapped around the given fixed name and address.
	 */
	EmailPopulatingBuilder withReplyTo(@Nullable String fixedName, @NotNull InternetAddress replyToAddress);
	
	/**
	 * Sets the <em>replyTo</em> address of this email with given {@link Recipient} (ignoring its {@link Message.RecipientType} if provided).
	 * <p>
	 * If provided, email clients should prioritize the <em>replyTo</em> recipient over the <em>from</em> recipient when replying to this email.
	 *
	 * @param recipient Preconfigured recipient which includes optional name and mandatory email address.
	 *
	 * @see #withReplyTo(String, String)
	 */
	EmailPopulatingBuilder withReplyTo(@Nullable Recipient recipient);
	
	/**
	 * Delegates to {@link #withBounceTo(Recipient)} with a new {@link Recipient} wrapped around the email address (or null if missing).
	 *
	 * @param bounceToAddress The address of the receiver of the bounced email
	 */
	@Cli.ExcludeApi(reason = "API is subset of another API")
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withBounceTo(@Nullable String bounceToAddress);
	
	/**
	 * Delegates to {@link #withBounceTo(Recipient)} with a new {@link Recipient} wrapped around the given name and email address.
	 *
	 * @param name Name of the receiver of the bounced email
	 * @param bounceToAddress The address of the receiver of the bounced email
	 */
	EmailPopulatingBuilder withBounceTo(@Nullable String name, @NotNull String bounceToAddress);
	
	/**
	 * Delegates to {@link #withBounceTo(Recipient)} with a new {@link Recipient} wrapped around the given address.
	 */
	@SuppressWarnings("UnusedReturnValue")
	EmailPopulatingBuilder withBounceTo(@NotNull InternetAddress bounceToAddress);
	
	/**
	 * Delegates to {@link #withBounceTo(Recipient)} with a new {@link Recipient} wrapped around the given fixed name and address.
	 */
	@Cli.ExcludeApi(reason = "Method is not detailed enough for CLI")
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withBounceTo(@Nullable String name, @NotNull InternetAddress bounceToAddress);
	
	/**
	 * Sets the <em>bounceTo</em> address of this email with given {@link Recipient} (ignoring its {@link Message.RecipientType} if provided).
	 * <p>
	 * If provided, SMTP server should return bounced emails to this address. This is also known as the {@code Return-Path} (or <em>Envelope
	 * FROM</em>).
	 *
	 * @param recipient Preconfigured recipient which includes optional name and mandatory email address.
	 *
	 * @see #withBounceTo(String, String)
	 */
	EmailPopulatingBuilder withBounceTo(@Nullable Recipient recipient);
	
	/**
	 * Sets the optional subject of this email.
	 *
	 * @param subject Optional text to be used in the subject field of the email.
	 */
	EmailPopulatingBuilder withSubject(@Nullable String subject);
	
	/**
	 * Delegates to {@link #withPlainText(String)}.
	 *
	 * @param textFile Plain text to set as email body (overwrites any previous plain text body). If no HTML body is included as well, plain text
	 *                would be used instead by the email client.
	 */
	@Cli.OptionNameOverride("withPlainTextFromFile")
	EmailPopulatingBuilder withPlainText(@NotNull File textFile);
	
	/**
	 * Sets the optional email message body in plain text.
	 * <p>
	 * Both text and HTML can be provided, which will  be offered to the email client as alternative content. Email clients that support it, will
	 * favor HTML over plain text and ignore the text body completely.
	 *
	 * @param text Plain text to set as email body (overwrites any previous plain text body). If no HTML body is included as well, plain text
	 *                would be used instead by the email client.
	 *
	 * @see #withPlainText(File)
	 * @see #prependText(String)
	 * @see #appendText(String)
	 */
	EmailPopulatingBuilder withPlainText(@Nullable String text);
	
	/**
	 * Delegates to {@link #prependText(String)}.
	 *
	 * @param textFile The plain text to prepend to whatever plain text is already there.
	 */
	@Cli.OptionNameOverride("prependTextFromFile")
	@SuppressWarnings("unused")
	EmailPopulatingBuilder prependText(@NotNull File textFile);
	
	/**
	 * Prepends text to the current plain text body (or starts it if plain text body is missing).
	 *
	 * @param text The plain text to prepend to whatever plain text is already there.
	 *
	 * @see #prependText(File)
	 * @see #appendText(String)
	 * @see #withPlainText(String)
	 */
	EmailPopulatingBuilder prependText(@NotNull String text);
	
	/**
	 * Delegates to {@link #appendText(String)}.
	 *
	 * @param textFile The plain text to append to whatever plain text is already there.
	 */
	@Cli.OptionNameOverride("appendTextFromFile")
	@SuppressWarnings("unused")
	EmailPopulatingBuilder appendText(@NotNull File textFile);
	
	/**
	 * Appends text to the current plain text body (or starts it if plain text body is missing).
	 *
	 * @param text The plain text to append to whatever plain text is already there.
	 *
	 * @see #appendText(File)
	 * @see #prependText(String)
	 * @see #withPlainText(String)
	 */
	EmailPopulatingBuilder appendText(@NotNull String text);
	
	/**
	 * Delegates to {@link #withHTMLText(String)}.
	 *
	 * @param textHTMLFile HTML text to set as email body (overwrites any previous HTML text body). If no HTML body is included, plain text
	 *                would be used instead by the email client if provided.
	 */
	@Cli.OptionNameOverride("withHTMLTextFromFile")
	EmailPopulatingBuilder withHTMLText(@NotNull File textHTMLFile);
	
	/**
	 * Sets the optional email message body in HTML text.
	 * <p>
	 * Both text and HTML can be provided, which will  be offered to the email client as alternative content. Email clients that support it, will
	 * favor HTML over plain text and ignore the text body completely.
	 *
	 * @param textHTML HTML text to set as email body (overwrites any previous HTML text body). If no HTML body is included, plain text
	 *                would be used instead by the email client if provided.
	 *
	 * @see #withHTMLText(File)
	 * @see #prependTextHTML(String)
	 * @see #appendTextHTML(String)
	 */
	EmailPopulatingBuilder withHTMLText(@Nullable String textHTML);
	
	/**
	 * Delegates to {@link #prependTextHTML(String)}.
	 *
	 * @param textHTMLFile The HTML text to prepend to whatever is already there in the body.
	 */
	@Cli.OptionNameOverride("prependTextHTMLFromFile")
	@SuppressWarnings("unused")
	EmailPopulatingBuilder prependTextHTML(@NotNull File textHTMLFile);
	
	/**
	 * Prepends HTML text to the current HTML text body (or starts it if HTML text body is missing).
	 *
	 * @param textHTML The HTML text to prepend to whatever is already there in the body.
	 *
	 * @see #prependTextHTML(File)
	 * @see #appendTextHTML(String)
	 * @see #withHTMLText(String)
	 */
	EmailPopulatingBuilder prependTextHTML(@NotNull String textHTML);
	
	/**
	 * Delegates to {@link #appendTextHTML(String)}.
	 *
	 * @param textHTMLFile The HTML text to append to whatever is already there in the body.
	 */
	@Cli.OptionNameOverride("appendTextHTMLFromFile")
	@SuppressWarnings("unused")
	EmailPopulatingBuilder appendTextHTML(@NotNull File textHTMLFile);
	
	/**
	 * Appends HTML text to the current HTML text body (or starts it if HTML text body is missing).
	 *
	 * @param textHTML The HTML text to append to whatever is already there in the body.
	 *
	 * @see #appendTextHTML(File)
	 * @see #prependTextHTML(String)
	 * @see #withHTMLText(String)
	 */
	EmailPopulatingBuilder appendTextHTML(@NotNull String textHTML);
	
	/**
	 * Sets the optional calendar details that clients such as Outlook might be able to handle. Will be set as alternative bodypart similar to
	 * {@link #withPlainText(String)} and {@link #withHTMLText(String)}.
	 *
	 * @param calendarMethod An RFC-2446 VEVENT calendar component method. Example: {@code PUBLISH, REQUEST, REPLY, ADD, CANCEL, REFRESH, COUNTER, DECLINECOUNTER}
	 * @param textCalendar free form text, which you should can produce with a library such as
	 *                        <a href="https://github.com/ical4j/ical4j/wiki/Examples">ical4j</a>.
	 *
	 * @see "The Test demo app in Simple Java Mail's source for a working example."
	 */
	EmailPopulatingBuilder withCalendarText(@NotNull CalendarMethod calendarMethod, @NotNull String textCalendar);

	/**
	 * Determines what encoding is applied to the text/html/iCalendar encoding in the MimeMessage/EML. Default is {@link ContentTransferEncoding#QUOTED_PRINTABLE}, which basicallt means plain
	 * text, so you can just read the content of the EML (if not encrypted).
	 * <p>
	 * However, you can choose another encoding as supported by Jakarta Mail. The list is quite extensive, but the most common alternative is base64. This might be useful for example for obfuscating
	 * the content to some extent.
	 *
	 * @param contentTransferEncoding The encoder to use for the text/html/iCalendar content.
	 */
	EmailPopulatingBuilder withContentTransferEncoding(@NotNull ContentTransferEncoding contentTransferEncoding);

	/**
	 * Delegates to {@link #withRecipients(Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	EmailPopulatingBuilder to(@NotNull Recipient @NotNull ...recipients);
	
	/**
	 * Delegates to {@link #withRecipients(Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	EmailPopulatingBuilder to(@NotNull Collection<Recipient> recipients);
	
	/**
	 * Delegates to {@link #withRecipients(String, boolean, Collection, Message.RecipientType)}, with <code>recipientType=</code>{@link Message.RecipientType#TO} and
	 * <code>fixedName=true</code> assigning or overwriting existing names with the provided name.
	 *
	 * @param name               The optional name of the TO receiver(s) of the email. If multiples addresses are provided, all addresses will be in
	 *                           this same name.
	 * @param oneOrMoreAddresses Single RFC2822 address or delimited list of RFC2822 addresses of TO receiver(s). Any names included are ignored if a
	 *                           name was provided. Examples:
	 *                           <ul>
	 *                           <li>lolly.pop@pretzelfun.com</li>
	 *                           <li>Lolly Pop&lt;lolly.pop@pretzelfun.com&gt;</li>
	 *                           <li>Moonpie &lt;moonpie@pies.com&gt;;Daisy &lt;daisy@pies.com&gt;</li>
	 *                           <li>a1@b1.c1,a2@b2.c2,a3@b3.c3</li>
	 *                           </ul>
	 */
	EmailPopulatingBuilder to(@Nullable String name, String oneOrMoreAddresses);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO} and empty default name.
	 *
	 * @param oneOrMoreAddresses Single RFC2822 address or delimited list of RFC2822 addresses. Examples:
	 *                           <ul>
	 *                           <li>lolly.pop@pretzelfun.com</li>
	 *                           <li>Lolly Pop&lt;lolly.pop@pretzelfun.com&gt;</li>
	 *                           <li>Moonpie &lt;moonpie@pies.com&gt;;Daisy &lt;daisy@pies.com&gt;</li>
	 *                           <li>a1@b1.c1,a2@b2.c2,a3@b3.c3</li>
	 *                           </ul>
	 */
	@Cli.ExcludeApi(reason = "API is subset of another API method")
	EmailPopulatingBuilder to(@NotNull String oneOrMoreAddresses);
	
	/**
	 * Alias for {@link #toWithFixedName(String, String...)}.
	 */
	EmailPopulatingBuilder to(@Nullable String name, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Alias for {@link #toWithFixedName(String, Collection)}.
	 */
	EmailPopulatingBuilder to(@Nullable String name, @NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder toMultiple(@NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder toMultiple(@NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	EmailPopulatingBuilder toWithFixedName(@Nullable String name, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	EmailPopulatingBuilder toWithDefaultName(@NotNull String name, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	EmailPopulatingBuilder toWithFixedName(@Nullable String name, @NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder toWithDefaultName(@NotNull String name, @NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Alias for {@link #toAddressesWithFixedName(String, InternetAddress...)}.
	 */
	EmailPopulatingBuilder to(@Nullable String name, InternetAddress address);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO} and empty default name.
	 */
	@SuppressWarnings("UnusedReturnValue")
	EmailPopulatingBuilder to(@NotNull InternetAddress address);
	
	/**
	 * Alias for {@link #toAddressesWithFixedName(String, InternetAddress...)}.
	 */
	EmailPopulatingBuilder to(@Nullable String name, @NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Alias for {@link #toAddressesWithFixedName(String, Collection)}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder toAddresses(@Nullable String name, @NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder toMultiple(@NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder toMultipleAddresses(@NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	EmailPopulatingBuilder toAddressesWithFixedName(@Nullable String name, @NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder toAddressesWithDefaultName(@NotNull String name, @NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	EmailPopulatingBuilder toAddressesWithFixedName(@Nullable String name, @NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#TO}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder toAddressesWithDefaultName(@NotNull String name, @NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withRecipients(Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	EmailPopulatingBuilder cc(@NotNull Recipient @NotNull ...recipients);
	
	/**
	 * Delegates to {@link #withRecipients(Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	EmailPopulatingBuilder cc(@NotNull Collection<Recipient> recipients);
	
	/**
	 * Delegates to {@link #withRecipients(String, boolean, Collection, Message.RecipientType)}, with <code>recipientType=</code>{@link Message.RecipientType#CC}
	 * and <code>fixedName=true</code> assigning or overwriting existing names with the provided name.
	 *
	 * @param name               The optional name of the CC receiver(s) of the email. If multiples addresses are provided, all addresses will be in
	 *                           this same name.
	 * @param oneOrMoreAddresses Single RFC2822 address or delimited list of RFC2822 addresses of CC receiver(s). Any names included are ignored if a
	 *                           name was provided. Examples:
	 *                           <ul>
	 *                           <li>lolly.pop@pretzelfun.com</li>
	 *                           <li>Lolly Pop&lt;lolly.pop@pretzelfun.com&gt;</li>
	 *                           <li>Moonpie &lt;moonpie@pies.com&gt;;Daisy &lt;daisy@pies.com&gt;</li>
	 *                           <li>a1@b1.c1,a2@b2.c2,a3@b3.c3</li>
	 *                           </ul>
	 */
	EmailPopulatingBuilder cc(@Nullable String name, String oneOrMoreAddresses);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC} and empty default name.
	 *
	 * @param oneOrMoreAddresses Single RFC2822 address or delimited list of RFC2822 addresses. Examples:
	 *                           <ul>
	 *                           <li>lolly.pop@pretzelfun.com</li>
	 *                           <li>Lolly Pop&lt;lolly.pop@pretzelfun.com&gt;</li>
	 *                           <li>Moonpie &lt;moonpie@pies.com&gt;;Daisy &lt;daisy@pies.com&gt;</li>
	 *                           <li>a1@b1.c1,a2@b2.c2,a3@b3.c3</li>
	 *                           </ul>
	 */
	@Cli.ExcludeApi(reason = "API is subset of another API method")
	EmailPopulatingBuilder cc(@NotNull String oneOrMoreAddresses);
	
	/**
	 * Alias for {@link #ccWithFixedName(String, String...)}.
	 */
	EmailPopulatingBuilder cc(@Nullable String name, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Alias for {@link #ccWithFixedName(String, Collection)}.
	 */
	EmailPopulatingBuilder cc(@Nullable String name, @NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC} and empty default name.
	 */
	@SuppressWarnings("UnusedReturnValue")
	EmailPopulatingBuilder ccMultiple(@NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder ccAddresses(@NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	EmailPopulatingBuilder ccWithFixedName(@Nullable String name, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	EmailPopulatingBuilder ccWithDefaultName(@NotNull String name, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	EmailPopulatingBuilder ccWithFixedName(@Nullable String name, @NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder ccWithDefaultName(@NotNull String name, @NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Alias for {@link #ccAddressesWithFixedName(String, InternetAddress...)}.
	 */
	EmailPopulatingBuilder cc(@Nullable String name, InternetAddress address);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC} and empty default name.
	 */
	@SuppressWarnings("UnusedReturnValue")
	EmailPopulatingBuilder cc(@NotNull InternetAddress address);
	
	/**
	 * Alias for {@link #ccAddressesWithFixedName(String, InternetAddress...)}.
	 */
	EmailPopulatingBuilder cc(@Nullable String name, @NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Alias for {@link #ccAddressesWithFixedName(String, Collection)}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder ccAddresses(@Nullable String name, @NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder ccMultiple(@NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder ccMultipleAddresses(@NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	EmailPopulatingBuilder ccAddressesWithFixedName(@Nullable String name, @NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder ccAddressesWithDefaultName(@NotNull String name, @NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	EmailPopulatingBuilder ccAddressesWithFixedName(@Nullable String name, @NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#CC}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder ccAddressesWithDefaultName(@NotNull String name, @NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withRecipients(Collection, Message.RecipientType)} with <code>fixedRecipientType=</code>{@link Message.RecipientType#BCC}.
	 **/
	EmailPopulatingBuilder bcc(@NotNull Recipient @NotNull ...recipients);
	
	/**
	 * Delegates to {@link #withRecipients(Collection, Message.RecipientType)} with <code>fixedRecipientType=</code>{@link Message.RecipientType#BCC}.
	 **/
	EmailPopulatingBuilder bcc(@NotNull Collection<Recipient> recipients);
	
	/**
	 * Delegates to {@link #withRecipients(String, boolean, Collection, Message.RecipientType)}, with <code>recipientType=</code>{@link Message.RecipientType#BCC}
	 * and <code>fixedName=true</code> assigning or overwriting existing names with the provided name.
	 *
	 * @param name               The optional name of the BCC receiver(s) of the email. If multiples addresses are provided, all addresses will be in
	 *                           this same name.
	 * @param oneOrMoreAddresses Single RFC2822 address or delimited list of RFC2822 addresses of BCC receiver(s). Any names included are ignored if a
	 *                           name was provided. Examples:
	 *                           <ul>
	 *                           <li>lolly.pop@pretzelfun.com</li>
	 *                           <li>Lolly Pop&lt;lolly.pop@pretzelfun.com&gt;</li>
	 *                           <li>Moonpie &lt;moonpie@pies.com&gt;;Daisy &lt;daisy@pies.com&gt;</li>
	 *                           <li>a1@b1.c1,a2@b2.c2,a3@b3.c3</li>
	 *                           </ul>
	 */
	EmailPopulatingBuilder bcc(@Nullable String name, String oneOrMoreAddresses);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC} and empty default name.
	 *
	 * @param oneOrMoreAddresses Single RFC2822 address or delimited list of RFC2822 addresses. Examples:
	 *                           <ul>
	 *                           <li>lolly.pop@pretzelfun.com</li>
	 *                           <li>Lolly Pop&lt;lolly.pop@pretzelfun.com&gt;</li>
	 *                           <li>Moonpie &lt;moonpie@pies.com&gt;;Daisy &lt;daisy@pies.com&gt;</li>
	 *                           <li>a1@b1.c1,a2@b2.c2,a3@b3.c3</li>
	 *                           </ul>
	 */
	@Cli.ExcludeApi(reason = "API is subset of another API")
	EmailPopulatingBuilder bcc(@NotNull String oneOrMoreAddresses);
	
	/**
	 * Alias for {@link #bccWithFixedName(String, String...)}.
	 */
	EmailPopulatingBuilder bcc(@Nullable String name, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Alias for {@link #bccWithFixedName(String, Collection)}.
	 */
	EmailPopulatingBuilder bcc(@Nullable String name, @NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder bccMultiple(@NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder bccAddresses(@NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC}.
	 */
	EmailPopulatingBuilder bccWithFixedName(@Nullable String name, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC}.
	 */
	EmailPopulatingBuilder bccWithDefaultName(@NotNull String name, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC}.
	 */
	EmailPopulatingBuilder bccWithFixedName(@Nullable String name, @NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipientsWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder bccWithDefaultName(@NotNull String name, @NotNull Collection<String> oneOrMoreAddressesEach);
	
	/**
	 * Alias for {@link #bccAddressesWithFixedName(String, InternetAddress...)}.
	 */
	EmailPopulatingBuilder bcc(@Nullable String name, InternetAddress address);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC} and empty default name.
	 */
	@SuppressWarnings("UnusedReturnValue")
	EmailPopulatingBuilder bcc(@NotNull InternetAddress address);
	
	/**
	 * Alias for {@link #bccAddressesWithFixedName(String, InternetAddress...)}.
	 */
	EmailPopulatingBuilder bcc(@Nullable String name, @NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Alias for {@link #bccAddressesWithFixedName(String, Collection)}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder bccAddresses(@Nullable String name, @NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder bccMultiple(@NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC} and empty default name.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder bccMultipleAddresses(@NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC}.
	 */
	EmailPopulatingBuilder bccAddressesWithFixedName(@Nullable String name, @NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder bccAddressesWithDefaultName(@NotNull String name, @NotNull InternetAddress @NotNull ...addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithFixedName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC}.
	 */
	EmailPopulatingBuilder bccAddressesWithFixedName(@Nullable String name, @NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withAddressesWithDefaultName(String, Collection, Message.RecipientType)} with <code>recipientType=</code>{@link Message.RecipientType#BCC}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder bccAddressesWithDefaultName(@NotNull String name, @NotNull Collection<InternetAddress> addresses);
	
	/**
	 * Delegates to {@link #withRecipients(String, boolean, Collection, Message.RecipientType)}, leaving existing names intact and defaulting when missing.
	 */
	@NotNull
	EmailPopulatingBuilder withRecipientsWithDefaultName(@Nullable String defaultName, @NotNull Collection<String> oneOrMoreAddressesEach, @Nullable Message.RecipientType recipientType);
	
	/**
	 * Delegates to {@link #withRecipients(String, boolean, Collection, Message.RecipientType)}, with <code>fixedName=true</code>
	 * assigning or overwriting existing names with the provided name.
	 */
	@NotNull
	EmailPopulatingBuilder withRecipientsWithFixedName(@Nullable String fixedName, @NotNull Collection<String> oneOrMoreAddressesEach, @Nullable Message.RecipientType recipientType);
	
	/**
	 * Delegates to {@link #withRecipients(String, boolean, Collection, Message.RecipientType)}.
	 */
	@NotNull
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withRecipientsWithDefaultName(@Nullable String name, @Nullable Message.RecipientType recipientType, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipients(String, boolean, Collection, Message.RecipientType)}.
	 */
	@NotNull
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withRecipientsWithFixedName(@Nullable String name, @Nullable Message.RecipientType recipientType, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipients(String, boolean, Collection, Message.RecipientType)}.
	 */
	@NotNull
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withRecipients(@Nullable String name, boolean fixedName, @Nullable Message.RecipientType recipientType, @NotNull String @NotNull ...oneOrMoreAddressesEach);
	
	/**
	 * Delegates to {@link #withRecipient(Recipient)} for each address found in not just the collection, but also in every individual address string
	 * that is in the collection.
	 *
	 * @param fixedName              Indicates whether the provided name should be applied to all addresses, or only to those where a name is
	 *                               missing.
	 * @param oneOrMoreAddressesEach Collection of addresses. Each entry itself can be a delimited list of RFC2822 addresses. Examples:
	 *                               <ul>
	 *                               <li>lolly.pop@pretzelfun.com</li>
	 *                               <li>Moonpie &lt;moonpie@pies.com&gt;;Daisy &lt;daisy@pies.com&gt;</li>
	 *                               <li>a1@b1.c1,a2@b2.c2,a3@b3.c3</li>
	 *                               </ul>
	 */
	@NotNull
	EmailPopulatingBuilder withRecipients(@Nullable String name, boolean fixedName, @NotNull Collection<String> oneOrMoreAddressesEach, @Nullable Message.RecipientType recipientType);
	
	/**
	 * Delegates to {@link #withAddresses(String, boolean, Collection, Message.RecipientType)}, leaving existing names intact and defaulting when missing.
	 */
	@NotNull
	EmailPopulatingBuilder withAddressesWithDefaultName(@Nullable String defaultName, @NotNull Collection<InternetAddress> addresses, @Nullable Message.RecipientType recipientType);
	
	/**
	 * Delegates to {@link #withAddresses(String, boolean, Collection, Message.RecipientType)}, with <code>fixedName=true</code>
	 * assigning or overwriting existing names with the provided name.
	 */
	@NotNull
	EmailPopulatingBuilder withAddressesWithFixedName(@Nullable String fixedName, @NotNull Collection<InternetAddress> addresses, @Nullable Message.RecipientType recipientType);
	
	/**
	 * Delegates to {@link #withRecipient(String, String, Message.RecipientType)} for each address in the provided collection.
	 *
	 * @param fixedName Indicates whether the provided name should be applied to all addresses, or only to those where a name is missing.
	 */
	@NotNull
	EmailPopulatingBuilder withAddresses(@Nullable String name, boolean fixedName, @NotNull Collection<InternetAddress> addresses, @Nullable Message.RecipientType recipientType);
	
	/**
	 * Delegates to {@link #withRecipients(Collection, Message.RecipientType)} with {@link Message.RecipientType} left empty (so it will use the original values).
	 */
	@SuppressWarnings("UnusedReturnValue")
	EmailPopulatingBuilder withRecipients(@NotNull Collection<Recipient> recipients);
	
	/**
	 * Delegates to {@link #withRecipients(Collection, Message.RecipientType)} with {@link Message.RecipientType} left empty (so it will use the original values).
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withRecipients(@NotNull Recipient @NotNull ...recipients);
	
	/**
	 * Delegates to {@link #withRecipient(String, String, Message.RecipientType)} for each recipient in the provided collection, optionally fixing the
	 * recipientType for all recipients to the provided type.
	 *
	 * @param fixedRecipientType Optional. Fixes all recipients to the given type. If omitted, the types are not removed, but kept as-is.
	 */
	@NotNull
	EmailPopulatingBuilder withRecipients(@NotNull Collection<Recipient> recipients, @Nullable Message.RecipientType fixedRecipientType);
	
	/**
	 * Delegates to {@link #withRecipient(String, String, Message.RecipientType)} with the name omitted.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withRecipient(@NotNull String singleAddress, @Nullable Message.RecipientType recipientType);
	
	/**
	 * Delegates to {@link #withRecipient(String, boolean, String, Message.RecipientType)} with the name omitted and fixedName = true.
	 */
	EmailPopulatingBuilder withRecipient(@Nullable String name, @NotNull String singleAddress, @Nullable Message.RecipientType recipientType);
	
	/**
	 * Adds a new {@link Recipient} instance with the given name, address and {@link Message.RecipientType}.
	 * <p>
	 * Note that the email address must be a single address according to RFC2822 format. Name can be provided explicitly or as part of the RFC2822 email
	 * address or omitted completely. If provided as method argument, the name overrides any nested name.
	 *
	 * @param name          Optional explicit name. Can be included in the email address instead, or omitted completely. A name will show as {@code
	 *                      "Name Here <address@domain.com>"}
	 * @param singleAddress A single address according to RFC2822 format with or without personal name.
	 * @param recipientType Optional type of recipient. This is needed for TO, CC and BCC, but not for <em>bounceTo</em>, <em>returnReceiptTo</em>,
	 *                      <em>replyTo</em>, <em>from</em> etc.
	 */
	EmailPopulatingBuilder withRecipient(@Nullable String name, boolean fixedName, @NotNull String singleAddress, @Nullable Message.RecipientType recipientType);
	
	/**
	 * Adds a new {@link Recipient} instance as copy of the provided recipient (copying name, address and {@link Message.RecipientType}).
	 * <p>
	 * Note that the email address must be a single address according to RFC2822 format. Name can be provided explicitly or as part of the RFC2822 email
	 * address or omitted completely.
	 */
	EmailPopulatingBuilder withRecipient(@NotNull Recipient recipient);

	/**
	 * Enables auto resolution of file datasources for embedded images.
	 * <p>
	 * Normally, you would manually mark up your HTML with images using {@code cid:<some_id>} and then add an embedded image
	 * resource with the same name ({@code emailBuilder.withEmbeddedImage(..)}). With auto-file-resolution, you can just
	 * refer to the file instead and the data will be included dynamically with a generated <em>cid</em>.
	 *
	 * @param embeddedImageAutoResolutionForFiles Enables auto resolution of file datasources for embedded images.
	 *
	 * @see #withEmbeddedImageBaseDir(String)
	 * @see #allowingEmbeddedImageOutsideBaseDir(boolean)
	 */
	EmailPopulatingBuilder withEmbeddedImageAutoResolutionForFiles(final boolean embeddedImageAutoResolutionForFiles);

	/**
	 * Enables auto resolution of classpath datasources for embedded images.
	 * <p>
	 * Normally, you would manually mark up your HTML with images using {@code cid:<some_id>} and then add an embedded image
	 * resource with the same name ({@code emailBuilder.withEmbeddedImage(..)}). With auto-classpath-resolution, you can just
	 * refer to the resource on the classpath instead and the data will be included dynamically with a generated <em>cid</em>.
	 *
	 * @param embeddedImageAutoResolutionForClassPathResources Enables auto resolution of classpath datasources for embedded images.
	 *
	 * @see #withEmbeddedImageBaseClassPath(String)
	 * @see #allowingEmbeddedImageOutsideBaseClassPath(boolean)
	 */
	EmailPopulatingBuilder withEmbeddedImageAutoResolutionForClassPathResources(final boolean embeddedImageAutoResolutionForClassPathResources);

	/**
	 * Enables auto resolution of URL's for embedded images.
	 * <p>
	 * Normally, you would manually mark up your HTML with images using {@code cid:<some_id>} and then add an embedded image
	 * resource with the same name ({@code emailBuilder.withEmbeddedImage(..)}). With auto-URL-resolution, you can just
	 * refer to the hosted image instead and the data will be downloaded and included dynamically with a generated <em>cid</em>.
	 *
	 * @param embeddedImageAutoResolutionForURLs Enables auto resolution of URL's for embedded images.
	 *
	 * @see #withEmbeddedImageBaseUrl(String)
	 * @see #withEmbeddedImageBaseUrl(URL)
	 * @see #allowingEmbeddedImageOutsideBaseUrl(boolean)
	 */
	EmailPopulatingBuilder withEmbeddedImageAutoResolutionForURLs(final boolean embeddedImageAutoResolutionForURLs);

	/**
	 * Sets the base folder used when resolving images sources in HTML text. Without this, the folder needs to be an absolute path (or a classpath/url resource).
	 * <p>
	 * Generally you would manually use src="cid:image_name", but files and url's will be located as well dynamically.
	 *
	 * @param embeddedImageBaseDir The base folder used when resolving images sources in HTML text.
	 *
	 * @see #withEmbeddedImageAutoResolutionForFiles(boolean)
	 * @see #allowingEmbeddedImageOutsideBaseDir(boolean)
	 */
	EmailPopulatingBuilder withEmbeddedImageBaseDir(@NotNull final String embeddedImageBaseDir);

	/**
	 * Sets the classpath base used when resolving images sources in HTML text. Without this, the resource needs to be an absolute path (or a file/url resource).
	 * <p>
	 * Generally you would manually use src="cid:image_name", but files and url's will be located as well dynamically.
	 *
	 * @param embeddedImageBaseClassPath The classpath base used when resolving images sources in HTML text.
	 *
	 * @see #withEmbeddedImageAutoResolutionForClassPathResources(boolean)
	 * @see #allowingEmbeddedImageOutsideBaseClassPath(boolean)
	 */
	EmailPopulatingBuilder withEmbeddedImageBaseClassPath(@NotNull final String embeddedImageBaseClassPath);

	/**
	 * Delegates to {@link #withEmbeddedImageBaseUrl(URL)}.
	 *
	 * @param embeddedImageBaseUrl The base URL used when resolving images sources in HTML text.
	 *
	 * @see #withEmbeddedImageAutoResolutionForURLs(boolean)
	 * @see #allowingEmbeddedImageOutsideBaseUrl(boolean)
	 */
	EmailPopulatingBuilder withEmbeddedImageBaseUrl(@NotNull final String embeddedImageBaseUrl);

	/**
	 * Sets the base URL used when resolving images sources in HTML text. Without this, the resource needs to be an absolute URL (or a file/classpath resource).
	 * <p>
	 * Generally you would manually use src="cid:image_name", but files and url's will be located as well dynamically.
	 *
	 * @param embeddedImageBaseUrl The base URL used when resolving images sources in HTML text.
	 *
	 * @see #withEmbeddedImageAutoResolutionForURLs(boolean)
	 * @see #allowingEmbeddedImageOutsideBaseUrl(boolean)
	 */
	@Cli.ExcludeApi(reason = "delegated method is an identical api from CLI point of view")
	EmailPopulatingBuilder withEmbeddedImageBaseUrl(@NotNull final URL embeddedImageBaseUrl);

	/**
	 * Dictates whether files will be resolved for embedded images when they are not nested under the baseDir (if baseDir is set).
	 *
	 * @param allowEmbeddedImageOutsideBaseDir Whether files should be resolved that reside outside the baseDir (if set)
	 *
	 * @see #withEmbeddedImageAutoResolutionForFiles(boolean)
	 * @see #withEmbeddedImageBaseDir(String)
	 */
	EmailPopulatingBuilder allowingEmbeddedImageOutsideBaseDir(final boolean allowEmbeddedImageOutsideBaseDir);

	/**
	 * Dictates whether sources will be resolved for embedded images when they are not nested under the baseClassPath (if baseClassPath is set).
	 *
	 * @param allowEmbeddedImageOutsideBaseClassPath Whether image sources should be resolved that reside outside the baseClassPath (if set)
	 *
	 * @see #withEmbeddedImageAutoResolutionForClassPathResources(boolean)
	 * @see #withEmbeddedImageBaseClassPath(String)
	 */
	EmailPopulatingBuilder allowingEmbeddedImageOutsideBaseClassPath(final boolean allowEmbeddedImageOutsideBaseClassPath);

	/**
	 * Dictates whether url's will be resolved for embedded images when they are not nested under the baseUrl (if baseUrl is set).
	 *
	 * @param allowEmbeddedImageOutsideBaseUrl Whether url's should be resolved that reside outside the baseUrl (if set)
	 *
	 * @see #withEmbeddedImageAutoResolutionForURLs(boolean)
	 * @see #withEmbeddedImageBaseUrl(String)
	 * @see #withEmbeddedImageBaseUrl(URL)
	 */
	EmailPopulatingBuilder allowingEmbeddedImageOutsideBaseUrl(final boolean allowEmbeddedImageOutsideBaseUrl);

	/**
	 * When embedded image auto resolution is enabled, this option will make sure unresolved images sources result in an exception.
	 * <p>
	 * Not using this option effectively means a more lenient approach to image sources.
	 * <p>
	 * Note: It also allows you to work with URL's as image sources that can't be resolved at time of sending, but that makes sense
	 * when viewing the email in some client (e.g. relative url's).
	 *
	 * @param embeddedImageAutoResolutionMustBeSuccesful Whether auto resolution is enforced and bubbles up failure to do so.
	 */
	EmailPopulatingBuilder embeddedImageAutoResolutionMustBeSuccesful(final boolean embeddedImageAutoResolutionMustBeSuccesful);
	
	/**
	 * Delegates to {@link #withEmbeddedImage(String, DataSource)}, with a named {@link ByteArrayDataSource} created using the provided name, data and
	 * mimetype.
	 *
	 * @param name     The name of the image as being referred to from the message content body (e.g. 'signature').
	 * @param data     The byte data of the image to be embedded.
	 * @param mimetype The content type of the given data (e.g. "image/gif" or "image/jpeg").
	 */
	EmailPopulatingBuilder withEmbeddedImage(@NotNull String name, byte@NotNull[] data, @NotNull String mimetype);
	
	/**
	 * Adds image data to this email that can be referred to from the email HTML body. For adding images as attachment, refer to {@link
	 * #withAttachment(String, DataSource)} instead.
	 * <p>
	 * The provided {@link DataSource} is assumed to be of mimetype png, jpg or whatever the email client supports as valid image embedded in HTML
	 * content.
	 *
	 * @param name      The name of the image as being referred to from the message content body (e.g. 'src="cid:yourImageName"'). If not provided, the
	 *                  name of the given data source is used instead.
	 * @param imagedata The image data.
	 *
	 * @see EmailPopulatingBuilder#withEmbeddedImage(String, byte[], String)
	 * @see EmailPopulatingBuilder#withEmbeddedImages(List)
	 */
	EmailPopulatingBuilder withEmbeddedImage(@Nullable String name, @NotNull DataSource imagedata);
	
	/**
	 * Delegates to {@link #withEmbeddedImage(String, DataSource)} for each embedded image.
	 */
	EmailPopulatingBuilder withEmbeddedImages(@NotNull List<AttachmentResource> embeddedImages);
	
	/**
	 * Delegates to {@link #withHeader(String, Object)} for each header in the provided {@code Map}.
	 */
	<T> EmailPopulatingBuilder withHeaders(@NotNull Map<String, Collection<T>> headers);

	/**
	 * Delegates to {@link #withHeader(String, Object, boolean)} with <em>replaceHeader</em> set to {@code false}.
	 *
	 * @param name  The name of the header. Example: <code>withHeader("X-Priority", 2)</code>
	 * @param value The value of the header, which will be stored using {@link String#valueOf(Object)}.
	 */
	EmailPopulatingBuilder withHeader(@NotNull String name, @Nullable Object value);

	/**
	 * Adds a header which will be included in the email. The value is stored as a <code>String</code>. Can be directed to replace the headers collection of values.
	 *
	 * @param name  The name of the header. Example: <code>withHeader("X-Priority", 2)</code>
	 * @param value The value of the header, which will be stored using {@link String#valueOf(Object)}.
	 * @param replaceHeader Whether to add the value to an existing collection of values (if any) or create a new collection with only the given value.
	 *
	 * @see #withHeaders(Map)
	 */
	@Cli.ExcludeApi(reason = "this is a rare case, so let's not crowd the CLI with it")
	EmailPopulatingBuilder withHeader(@NotNull String name, @Nullable Object value, boolean replaceHeader);

	/**
	 * Delegates to {@link #withAttachment(String, byte[], String, String, ContentTransferEncoding)} with null-description and no forced content transfer encoding.
	 */
	EmailPopulatingBuilder withAttachment(@Nullable String name, byte@NotNull[] data, @NotNull String mimetype);

	/**
	 * Delegates to {@link #withAttachment(String, byte[], String, String, ContentTransferEncoding)} with no forced content transfer encoding.
	 */
	EmailPopulatingBuilder withAttachment(@Nullable String name, byte@NotNull[] data, @NotNull String mimetype, @Nullable String description);

	/**
	 * Delegates to {@link #withAttachment(String, DataSource)}, with a named {@link ByteArrayDataSource} created using the provided name, data and mimetype.
	 *
	 * @param name                    Optional name of the attachment (e.g. 'filename.ext'). If omitted, the internal name of the datasource is used. If that too is empty, a name will be generated
	 *                                using {@link java.util.UUID}.
	 * @param data                    The binary data of the attachment.
	 * @param mimetype                The content type of the given data (e.g. "plain/text", "image/gif" or "application/pdf").
	 * @param description             An optional description that will find its way in the MimeMEssage with the Content-Description header. This is rarely needed.
	 * @param contentTransferEncoding An optional encoder option to force the data encoding while in MimeMessage/EML format.
	 *
	 * @see #withAttachment(String, DataSource, String, ContentTransferEncoding)
	 * @see #withAttachments(List)
	 */
	EmailPopulatingBuilder withAttachment(@Nullable String name, byte@NotNull[] data, @NotNull String mimetype, @Nullable String description, @Nullable ContentTransferEncoding contentTransferEncoding);
	
	/**
	 * Delegates to {@link #withAttachment(String, DataSource, String, ContentTransferEncoding)} with null-description and no forced content transfer encoding.
	 *
	 * @param name                    Optional name of the attachment (e.g. 'filename.ext'). If omitted, the internal name of the datasource is used. If that too is empty, a name will be generated
	 *                                using {@link java.util.UUID}.
	 * @param filedata                The attachment data.
	 */
	EmailPopulatingBuilder withAttachment(@Nullable String name, @NotNull DataSource filedata);

	/**
	 * Delegates to {@link #withAttachment(String, DataSource, String, ContentTransferEncoding)} with no forced content transfer encoding.
	 *
	 * @param name                    Optional name of the attachment (e.g. 'filename.ext'). If omitted, the internal name of the datasource is used. If that too is empty, a name will be generated
	 *                                using {@link java.util.UUID}.
	 * @param filedata                The attachment data.
	 * @param description             An optional description that will find its way in the MimeMEssage with the Content-Description header. This is rarely needed.
	 */
	@Cli.OptionNameOverride("withDescribedAttachment")
	EmailPopulatingBuilder withAttachment(@Nullable String name, @NotNull DataSource filedata, @Nullable final String description);

	/**
	 * Adds an attachment to the email message, which will be shown in the email client as seperate files available for download or inline display if the client supports it (for example, most browsers
	 * these days display PDF's in a popup).
	 * <p>
	 * <strong>Note</strong>: for embedding images instead of attaching them for download, refer to {@link #withEmbeddedImage(String, DataSource)} instead.
	 *
	 * @param name                    Optional name of the attachment (e.g. 'filename.ext'). If omitted, the internal name of the datasource is used. If that too is empty, a name will be generated
	 *                                using {@link java.util.UUID}.
	 * @param filedata                The attachment data.
	 * @param description             An optional description that will find its way in the MimeMEssage with the Content-Description header. This is rarely needed.
	 * @param contentTransferEncoding An optional encoder option to force the data encoding while in MimeMessage/EML format.
	 *
	 * @see #withAttachment(String, DataSource, String, ContentTransferEncoding)
	 * @see #withAttachments(List)
	 */
	@Cli.OptionNameOverride("withEncodedDescribedAttachment")
	EmailPopulatingBuilder withAttachment(@Nullable String name, @NotNull DataSource filedata, @Nullable final String description, @Nullable final ContentTransferEncoding contentTransferEncoding);

	/**
	 * Delegates to {@link #withAttachment(String, DataSource)} for each attachment.
	 */
	EmailPopulatingBuilder withAttachments(@NotNull List<AttachmentResource> attachments);

	/**
	 * Primes this email for signing with a DKIM domain key. Actual signing is done when sending using a <code>Mailer</code>.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.DKIMModule#NAME}.
	 *
	 * @param dkimPrivateKey                            De key content used to sign for the sending party.
	 * @param signingDomain                             The domain being authorized to send.
	 * @param dkimSelector                              Additional domain specifier.
	 * @param excludedHeadersFromDkimDefaultSigningList Allows you to exclude headers being signed, as might be the case when another mail transfer agent. For example, Amazon SES doesn't want Message-ID and Date Headers to be signed as they have internal mechanisms to handle these headers.
	 *
	 * @see <a href="https://postmarkapp.com/guides/dkim">more on DKIM 1</a>
	 * @see <a href="https://github.com/markenwerk/java-utils-mail-dkim">more on DKIM 2</a>
	 * @see <a href="http://www.gettingemaildelivered.com/dkim-explained-how-to-set-up-and-use-domainkeys-identified-mail-effectively">more on DKIM 3</a>
	 * @see <a href="https://en.wikipedia.org/wiki/DomainKeys_Identified_Mail">more on DKIM 4</a>
	 * @see <a href="https://github.com/bbottema/dkim-verify">List of explanations all tags in a <em>DKIM-Signature</em> header</a>
	 * @see #signWithDomainKey(DkimConfig)
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder signWithDomainKey(byte@NotNull[] dkimPrivateKey, @NotNull String signingDomain, @NotNull String dkimSelector, @Nullable Set<String> excludedHeadersFromDkimDefaultSigningList);

	/**
	 * @see #signWithDomainKey(byte[], String, String, Set)
	 */
	@Cli.ExcludeApi(reason = "delegated method is an identical api from CLI point of view")
	@SuppressWarnings("unused")
	EmailPopulatingBuilder signWithDomainKey(@NotNull DkimConfig dkimConfig);
	
	/**
	 * Signs this email with an <a href="https://tools.ietf.org/html/rfc5751">S/MIME</a> signature, so the receiving client
	 * can verify whether the email content was tampered with.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.<br>
	 * <strong>Note:</strong> You can also configure your <code>Mailer</code> instance do sign all emails by default (also has better performance).
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/S/MIME">S/MIME on Wikipedia</a>
	 * @see <a href="https://www.globalsign.com/en/blog/what-is-s-mime/">Primer on S/MIME</a>
	 */
	@Cli.ExcludeApi(reason = "delegated method contains CLI compatible arguments")
	EmailPopulatingBuilder signWithSmime(@NotNull Pkcs12Config pkcs12Config);

	/**
	 * Delegates to {@link #signWithSmime(InputStream, String, String, String)}.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 *
	 * @param pkcs12StoreFile The key store file to use to find the indicated key
	 * @param storePassword The store's password
	 * @param keyAlias The name of the certificate in the key store to use
	 * @param keyPassword The password of the certificate
	 */
	EmailPopulatingBuilder signWithSmime(@NotNull File pkcs12StoreFile, @NotNull String storePassword, @NotNull String keyAlias, @NotNull String keyPassword);

	/**
	 * Delegates to {@link #signWithSmime(byte[], String, String, String)}.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 */
	@Cli.ExcludeApi(reason = "Is duplicate API from CLI point of view")
	EmailPopulatingBuilder signWithSmime(@NotNull InputStream pkcs12StoreStream, @NotNull String storePassword, @NotNull String keyAlias, @NotNull String keyPassword);

	/**
	 * Delegates to {@link #signWithSmime(Pkcs12Config)}.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 *
	 * @param pkcs12StoreData The key store file to use to find the indicated key
	 * @param storePassword The store's password
	 * @param keyAlias The name of the certificate in the key store to use
	 * @param keyPassword The password of the certificate
	 */
	@Cli.ExcludeApi(reason = "Is duplicate API from CLI point of view")
	EmailPopulatingBuilder signWithSmime(byte@NotNull[] pkcs12StoreData, @NotNull String storePassword, @NotNull String keyAlias, @NotNull String keyPassword);

	/**
	 * Delegates to {@link #encryptWithSmime(X509Certificate)} using the provided PEM file.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 *
	 * @param pemStream A PEM encoded file that will be read as X509Certificate.
	 */
	@Cli.ExcludeApi(reason = "Is duplicate API from CLI point of view")
	EmailPopulatingBuilder encryptWithSmime(@NotNull InputStream pemStream);

	/**
	 * Delegates to {@link #encryptWithSmime(InputStream)} using the provided PEM file.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 *
	 * @param pemFile A PEM encoded file that will be read as X509Certificate.
	 */
	@Cli.ExcludeApi(reason = "Is duplicate API from CLI point of view")
	EmailPopulatingBuilder encryptWithSmime(@NotNull String pemFile);

	/**
	 * Delegates to {@link #encryptWithSmime(InputStream)} using the provided PEM file.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 *
	 * @param pemFile A PEM encoded file that will be read as X509Certificate.
	 */
	@Cli.ExcludeApi(reason = "Is duplicate API from CLI point of view")
	EmailPopulatingBuilder encryptWithSmime(@NotNull File pemFile);

	/**
	 * Encrypts this email with a X509 certificate according to the <a href="https://tools.ietf.org/html/rfc5751">S/MIME spec</a>
	 * signature.
	 * <p>
	 * You can sign this email with the public key you received from your recipient. The recipient then is the only person that
	 * can decrypt the email with his or her private key.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 *
	 * @param x509Certificate The recipient's public key to use for encryption.
	 *
	 * @see <a href="https://en.wikipedia.org/wiki/S/MIME">S/MIME on Wikipedia</a>
	 * @see <a href="https://www.globalsign.com/en/blog/what-is-s-mime/">Primer on S/MIME</a>
	 * @see <a href="https://github.com/markenwerk/java-utils-mail-smime">Underlying library's documentation</a>
	 */
	EmailPopulatingBuilder encryptWithSmime(@NotNull X509Certificate x509Certificate);

	/**
	 * When the S/MIME module is loaded, S/MIME signed / encrypted attachments are decrypted and kept in a separate list. However
	 * if it is a single attachment and the actual attachment has mimetype "message/rfc822", it is assumes to be the message
	 * itself and by default will be merged with the top level email (basically overriding body, headers and attachments).
	 * <br>
	 * This API disables this behavior and stricly keeps all attachments as-is (still decrypted, but not merged with the email).
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 */
	EmailPopulatingBuilder notMergingSingleSMIMESignedAttachment();

	/**
	 * Indicates that we want to use the NPM flag {@code dispositionNotificationTo}. The actual address will default to the {@code replyToRecipient}
	 * first if set or else {@code fromRecipient} (the final address is determined when sending this email).
	 *
	 * @see #withDispositionNotificationTo(Recipient)
	 */
	@Cli.OptionNameOverride("withDispositionNotificationToEnabled")
	EmailPopulatingBuilder withDispositionNotificationTo();
	
	/**
	 * Delegates to {@link #withDispositionNotificationTo(Recipient)} with a new {@link Recipient} wrapped around the provided address.
	 *
	 * @param address The address of the receiver of the notification
	 */
	@Cli.ExcludeApi(reason = "API is subset of another API")
	EmailPopulatingBuilder withDispositionNotificationTo(@NotNull String address);
	
	/**
	 * Delegates to {@link #withDispositionNotificationTo(Recipient)} with a new {@link Recipient} wrapped around the provided name and address.
	 *
	 * @param name Optional name of the receiver of the notification
	 * @param address The address of the receiver of the notification
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withDispositionNotificationTo(@Nullable String name, @NotNull String address);
	
	/**
	 * Delegates to {@link #withDispositionNotificationTo(Recipient)} with a new {@link Recipient} wrapped around the provided address.
	 */
	@SuppressWarnings("UnusedReturnValue")
	EmailPopulatingBuilder withDispositionNotificationTo(@NotNull InternetAddress address);
	
	/**
	 * Delegates to {@link #withDispositionNotificationTo(Recipient)} with a new {@link Recipient} wrapped around the provided fixed name and address.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withDispositionNotificationTo(@Nullable String fixedName, @NotNull InternetAddress address);
	
	/**
	 * Indicates this email should use the <a href="https://tools.ietf.org/html/rfc8098">NPM flag "Disposition-Notification-To"</a> with the given
	 * preconfigred {@link Recipient}. This flag can be used to request a return receipt from the recipient to signal that the recipient has read the
	 * email.
	 * <p>
	 * This flag may be ignored by SMTP clients (for example gmail ignores it completely, while the Google Apps business suite honors it).
	 *
	 * @see #withDispositionNotificationTo()
	 * @see #withDispositionNotificationTo(String)
	 * @see #withDispositionNotificationTo(String, String)
	 */
	EmailPopulatingBuilder withDispositionNotificationTo(@NotNull Recipient recipient);
	
	/**
	 * Indicates that we want to use the flag {@code returnReceiptTo}. The actual address will default to the {@code replyToRecipient} first if set
	 * or else {@code fromRecipient} (the final address is determined when sending the email).
	 * <p>
	 * For more detailed information, refer to {@link #withReturnReceiptTo(Recipient)}.
	 */
	@Cli.OptionNameOverride("withReturnReceiptToEnabled")
	EmailPopulatingBuilder withReturnReceiptTo();
	
	/**
	 * Delegates to {@link #withReturnReceiptTo(Recipient)} with a new {@link Recipient} wrapped around the provided address.
	 *
	 * @param address The address of the receiver of the bounced email
	 */
	@Cli.ExcludeApi(reason = "API is subset of another API")
	EmailPopulatingBuilder withReturnReceiptTo(@NotNull String address);
	
	/**
	 * Delegates to {@link #withReturnReceiptTo(Recipient)} with a new {@link Recipient} wrapped around the provided name and address.
	 *
	 * @param name Name of the receiver of the receipt notification
	 * @param address The address of the receiver of the receipt notification
	 */
	EmailPopulatingBuilder withReturnReceiptTo(@Nullable String name, @NotNull String address);
	
	/**
	 * Delegates to {@link #withReturnReceiptTo(Recipient)} with a new {@link Recipient} wrapped around the provided address.
	 */
	@SuppressWarnings("UnusedReturnValue")
	EmailPopulatingBuilder withReturnReceiptTo(@NotNull InternetAddress address);
	
	/**
	 * Delegates to {@link #withReturnReceiptTo(Recipient)} with a new {@link Recipient} wrapped around the provided fixed name and address.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder withReturnReceiptTo(@Nullable String fixedName, @NotNull InternetAddress address);

	/**
	 * Indicates that this email should use the <a href="https://en.wikipedia.org/wiki/Return_receipt">RRT flag "Return-Receipt-To"</a> with the
	 * preconfigured {@link Recipient}. This flag can be used to request a notification from the SMTP server recipient to signal that the recipient
	 * has read the email.
	 * <p>
	 * This flag is rarely used, but your mail server / client might implement this flag to automatically send back a notification that the email was
	 * received on the mail server or opened in the client, depending on the chosen implementation.
	 */
	EmailPopulatingBuilder withReturnReceiptTo(@NotNull Recipient recipient);

	/**
	 * When an email is sent it is converted to a MimeMessage at which time the sent-date is filled with the current date. With this method
	 * this can be fixed to a date of choice.
	 * <p>
	 * <strong>Note:</strong> the <em>sent</em> date is user-controlled. Only when converting an email, Simple Java Mail might fill the sent-date.
	 *
	 * @param sentDate The date to use as sent date.
	 */
	EmailPopulatingBuilder fixingSentDate(@NotNull Date sentDate);

	/**
	 * Resets <em>id</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearId();
	
	/**
	 * Resets <em>fromRecipient</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearFromRecipient();
	
	/**
	 * Resets <em>replyToRecipient</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearReplyTo();
	
	/**
	 * Resets <em>bounceToRecipient</em> to empty.
	 */
	@SuppressWarnings("UnusedReturnValue")
	EmailPopulatingBuilder clearBounceTo();
	
	/**
	 * Resets <em>text</em> to empty.
	 */
	EmailPopulatingBuilder clearPlainText();
	
	/**
	 * Resets <em>textHTML</em> to empty.
	 */
	EmailPopulatingBuilder clearHTMLText();

	/**
	 * Resets <em>calendarText</em> to empty.
	 */
	EmailPopulatingBuilder clearCalendarText();

	/**
	 * Resets <em>contentTransferEncoding</em> to {@link ContentTransferEncoding#QUOTED_PRINTABLE}.
	 */
	EmailPopulatingBuilder clearContentTransferEncoding();
	
	/**
	 * Resets <em>subject</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearSubject();
	
	/**
	 * Resets <em>recipients</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearRecipients();

	/**
	 * Resets base dir for embedded images to {@code null}.
	 */
	EmailPopulatingBuilder clearEmbeddedImageBaseDir();

	/**
	 * Resets classpath base for embedded images to {@code null}.
	 */
	EmailPopulatingBuilder clearEmbeddedImageBaseClassPath();

	/**
	 * Resets base URL for embedded images to {@code null}.
	 */
	EmailPopulatingBuilder clearEmbeddedImageBaseUrl();

	/**
	 * Resets <em>embeddedImages</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearEmbeddedImages();
	
	/**
	 * Resets <em>attachments</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearAttachments();
	
	/**
	 * Resets <em>headers</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearHeaders();

	/**
	 * Resets all dkim properties to empty.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.DKIMModule#NAME}.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearDkim();

	/**
	 * For signing and encrypting this email when sending, resets all S/MIME properties to empty.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 *
	 * @see #signWithSmime(Pkcs12Config)
	 * @see #encryptWithSmime(X509Certificate)
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearSmime();
	
	/**
	 * Resets <em>dispositionNotificationTo</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearDispositionNotificationTo();
	
	/**
	 * Resets <em>returnReceiptTo</em> to empty.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearReturnReceiptTo();

	/**
	 * Clears the fixed <em>sent-date</em> so that the current date is used again at the time of sending.
	 */
	@SuppressWarnings("unused")
	EmailPopulatingBuilder clearSentDate();

	/**
	 * When readig and converting an email, this flag makes the behavior revert to the default merging
	 * behavior for single S/MIME signed attachments, which is that it <em>is</em> merged into the root message.
	 * <p>
	 * This can be useful when copying an {@link Email} that <em>was</em> merged (default behavior), to unmerge it.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 */
	EmailPopulatingBuilder clearSMIMESignedAttachmentMergingBehavior();

	/**
	 * @see #ignoringDefaults(boolean)
	 */
	boolean isIgnoreDefaults();

	/**
	 * @see #ignoringOverrides(boolean)
	 */
	boolean isIgnoreOverrides();

	/**
	 * @see #dontApplyDefaultValueFor(EmailProperty...)
	 */
	@Nullable
	Set<EmailProperty> getPropertiesNotToApplyDefaultValueFor();

	/**
	 * @see #dontApplyOverrideValueFor(EmailProperty...)
	 */
	@Nullable
	Set<EmailProperty> getPropertiesNotToApplyOverrideValueFor();

	/**
	 * @see #fixingMessageId(String)
	 */
	@Nullable
	String getId();
	
	/**
	 * @see #from(Recipient)
	 */
	@Nullable
	Recipient getFromRecipient();
	
	/**
	 * @see #withReplyTo(Recipient)
	 */
	@Nullable
	Recipient getReplyToRecipient();
	
	/**
	 * @see #withBounceTo(Recipient)
	 */
	@Nullable
	Recipient getBounceToRecipient();
	
	/**
	 * @see #withPlainText(String)
	 */
	@Nullable
	String getText();
	
	/**
	 * @see #withHTMLText(String)
	 */
	@Nullable
	String getTextHTML();
	
	/**
	 * @see #withCalendarText(CalendarMethod, String)
	 */
	@Nullable
	CalendarMethod getCalendarMethod();

	/**
	 * @see #withCalendarText(CalendarMethod, String)
	 */
	@Nullable
	String getTextCalendar();

	/**
	 * @see #withContentTransferEncoding(ContentTransferEncoding)
	 * @see #clearContentTransferEncoding()
	 */
	@Nullable
	ContentTransferEncoding getContentTransferEncoding();
	
	/**
	 * @see #withSubject(String)
	 */
	@Nullable
	String getSubject();
	
	/**
	 * @see #to(Recipient...)
	 * @see #cc(Recipient...)
	 * @see #bcc(Recipient...)
	 */
	@NotNull
	List<Recipient> getRecipients();
	
	/**
	 * @see #withEmbeddedImage(String, DataSource)
	 */
	@NotNull
	List<AttachmentResource> getEmbeddedImages();
	
	/**
	 * @see #withAttachment(String, DataSource)
	 */
	@NotNull
	List<AttachmentResource> getAttachments();


	/**
	 * If the S/MIME library is loaded, this method returns a copy list of the attachments, but with any signed
	 * attachments replaced with decrypted ones.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 */
	@NotNull
	List<AttachmentResource> getDecryptedAttachments();

	/**
	 * @see #withHeader(String, Object)
	 * @see EmailStartingBuilder#replyingTo(MimeMessage, boolean, String)
	 */
	@NotNull
	Map<String, Collection<String>> getHeaders();
	
	/**
	 * @see #signWithDomainKey(DkimConfig)
	 * @see #signWithDomainKey(byte[], String, String, Set)
	 */
	@Nullable
	DkimConfig getDkimConfig();
	
	/**
	 * @see #withDispositionNotificationTo()
	 * @see #withDispositionNotificationTo(Recipient)
	 */
	@Nullable
	Boolean getUseDispositionNotificationTo();
	
	/**
	 * @see #withDispositionNotificationTo()
	 * @see #withDispositionNotificationTo(Recipient)
	 */
	@Nullable
	Recipient getDispositionNotificationTo();
	
	/**
	 * @see #withReturnReceiptTo()
	 * @see #withReturnReceiptTo(Recipient)
	 */
	@Nullable
	Boolean getUseReturnReceiptTo();
	
	/**
	 * @see #withReturnReceiptTo()
	 * @see #withReturnReceiptTo(Recipient)
	 */
	@Nullable
	Recipient getReturnReceiptTo();
	
	/**
	 * @see EmailStartingBuilder#forwarding(MimeMessage)
	 */
	@Nullable
	MimeMessage getEmailToForward();

	/**
	 * If this Email instance was the result of a conversion in which the source message was S/MIME signed / encrypted,
	 * this field will be filled for historical purpose.
	 * <p>
	 * For example, you can use it to determine if the message was encrypted or signed and also who did the signing.
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 */
	@NotNull
	OriginalSmimeDetails getOriginalSmimeDetails();

	/**
	 * If the Email instance currently being built was the result of a conversion in which the source message was S/MIME
	 * signed / encrypted, this field will contain the decrypted MimeMessage instance.
	 * <p>
	 * By default, this message is merged into the parent email, as it is actually the same message (this behavior can be
	 * turned off with {@link #notMergingSingleSMIMESignedAttachment()}).
	 * <p>
	 * <strong>Note:</strong> this only works in combination with the {@value org.simplejavamail.internal.modules.SMIMEModule#NAME}.
	 */
	@Nullable
	Email getSmimeSignedEmail();

	/**
	 * @see #notMergingSingleSMIMESignedAttachment()
	 */
	boolean isMergeSingleSMIMESignedAttachment();

	/**
	 * @see EmailPopulatingBuilder#signWithSmime(Pkcs12Config)
	 * @see EmailPopulatingBuilder#signWithSmime(InputStream, String, String, String)
	 */
	@Nullable
	Pkcs12Config getPkcs12ConfigForSmimeSigning();

	/**
	 * @see EmailPopulatingBuilder#encryptWithSmime(X509Certificate)
	 * @see EmailPopulatingBuilder#encryptWithSmime(InputStream)
	 */
	@Nullable
	X509Certificate getX509CertificateForSmimeEncryption();

	/**
	 * @see EmailPopulatingBuilder#fixingSentDate(Date)
	 */
	@Nullable
	Date getSentDate();
}
