package de.symeda.sormas.backend.audit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.InvocationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.symeda.sormas.backend.auditlog.AuditContextProducer;
import de.symeda.sormas.backend.auditlog.AuditLogServiceBean;
import de.symeda.sormas.backend.campaign.CampaignService;
import de.symeda.sormas.backend.caze.CaseService;
import de.symeda.sormas.backend.common.AbstractDomainObject;
import de.symeda.sormas.backend.common.ConfigFacadeEjb;
import de.symeda.sormas.backend.contact.ContactService;
import de.symeda.sormas.backend.event.EventParticipantService;
import de.symeda.sormas.backend.event.EventService;
import de.symeda.sormas.backend.feature.FeatureConfigurationFacadeEjb;
import de.symeda.sormas.backend.i18n.I18nFacadeEjb;
import de.symeda.sormas.backend.immunization.DirectoryImmunizationService;
import de.symeda.sormas.backend.immunization.ImmunizationService;
import de.symeda.sormas.backend.infrastructure.continent.ContinentFacadeEjb;
import de.symeda.sormas.backend.infrastructure.subcontinent.SubcontinentFacadeEjb;
import de.symeda.sormas.backend.sample.PathogenTestService;
import de.symeda.sormas.backend.sample.SampleService;
import de.symeda.sormas.backend.travelentry.services.TravelEntryListService;
import de.symeda.sormas.backend.travelentry.services.TravelEntryService;
import de.symeda.sormas.backend.user.CurrentUserService;
import de.symeda.sormas.backend.user.UserFacadeEjb;

public class AuditLoggerInterceptor {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	@EJB
	AuditLoggerEjb.AuditLoggerEjbLocal auditLogger;

	/**
	 * Cache to track all classes that should be ignored and those who must be audited. False indicates audit, True ignore
	 */
	private static final Map<Class<?>, Boolean> shouldIgnoreClassCache;
	static {
		Map<Class<?>, Boolean> map = new HashMap<>();
		// explicitly add all services doing deletion here to bypass the local bean check
		map.put(CaseService.class, false);
		map.put(CampaignService.class, false);
		map.put(ContactService.class, false);
		map.put(DirectoryImmunizationService.class, false);
		map.put(EventParticipantService.class, false);
		map.put(EventService.class, false);
		map.put(ImmunizationService.class, false);
		map.put(PathogenTestService.class, false);
		map.put(SampleService.class, false);
		map.put(TravelEntryListService.class, false);
		map.put(TravelEntryService.class, false);
		shouldIgnoreClassCache = map;
	}

	/**
	 * Cache to track all classes which should be completely ignored for audit.
	 */
	private static final Set<Class<?>> ignoreAuditClasses = Collections.unmodifiableSet(
		new HashSet<>(
			Arrays.asList(
				FeatureConfigurationFacadeEjb.class,
				ConfigFacadeEjb.class,
				CurrentUserService.class,
				AuditContextProducer.class,
				AuditLogServiceBean.class,
				AuditLoggerEjb.class,
				I18nFacadeEjb.class)));

	/**
	 * Cache to track all methods which should be completely ignored.
	 */
	private static final Set<Method> ignoreAuditMethods;

	static {
		try {
			ignoreAuditMethods = Collections.unmodifiableSet(
				new HashSet<>(
					Arrays.asList(
						ContinentFacadeEjb.class.getMethod("getByDefaultName", String.class, boolean.class),
						SubcontinentFacadeEjb.class.getMethod("getByDefaultName", String.class, boolean.class),
						UserFacadeEjb.class.getMethod("getCurrentUser"),
						UserFacadeEjb.class.getMethod("getValidLoginRights", String.class, String.class))));
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Cache to track all local methods which should be audited.
	 */
	private static final Set<Method> allowedLocalAuditMethods;

	public static final String DELETE_PERMANENT = "deletePermanent";

	static {
		try {
			allowedLocalAuditMethods = Collections.unmodifiableSet(
				new HashSet<>(
					Arrays.asList(
						CaseService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						CampaignService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						ContactService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						DirectoryImmunizationService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						EventParticipantService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						EventService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						ImmunizationService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						PathogenTestService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						SampleService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						TravelEntryListService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class),
						TravelEntryService.class.getMethod(DELETE_PERMANENT, AbstractDomainObject.class)
					)));
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@AroundTimeout
	public Object logTimeout(InvocationContext context) throws Exception {
		if (AuditLoggerEjb.isLoggingDisabled()) {
			return context.proceed();
		}
		return backendAuditing(context, context.getMethod());
	}

	@AroundInvoke
	public Object logAudit(InvocationContext context) throws Exception {

		if (AuditLoggerEjb.isLoggingDisabled()) {
			return context.proceed();
		}

		Class<?> target = context.getTarget().getClass();

		if (ignoreAuditClasses.contains(target)) {
			// ignore certain classes for audit altogether. Statically populated cache.
			return context.proceed();
		}
		Method calledMethod = context.getMethod();

		// with this we ignore EJB calls which definitely originate from within the backend
		// as they can never be called direct from outside (i.e., remote) of the backend
		// expression yields true if it IS a local bean => should not be audited and ignored
		Boolean shouldIgnoreAudit = shouldIgnoreClassCache.computeIfAbsent(target, k -> target.getAnnotationsByType(LocalBean.class).length > 0);

		if (Boolean.TRUE.equals(shouldIgnoreAudit)) {
			// ignore local beans
			return context.proceed();
		}

		if (ignoreAuditMethods.contains(calledMethod) || !allowedLocalAuditMethods.contains(calledMethod)) {
			// ignore certain methods for audit altogether. Statically populated cache.
			return context.proceed();
		}

		return backendAuditing(context, calledMethod);
	}

	private Object backendAuditing(InvocationContext context, Method calledMethod) throws Exception {

		Object[] parameters = context.getParameters();
		Date start = Calendar.getInstance(TimeZone.getDefault()).getTime();

		Object result = context.proceed();

		Date end = Calendar.getInstance(TimeZone.getDefault()).getTime();

		auditLogger.logBackendCall(calledMethod, parameters, result, start, end);
		return result;
	}

}
