package housemate.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import housemate.constants.Role;
import housemate.constants.ServiceConfiguration;
import housemate.constants.Enum.*;
import housemate.constants.ImageType;
import housemate.entities.Image;
import housemate.entities.PackageServiceItem;
import housemate.entities.Period;
import housemate.entities.PeriodPriceConfig;
import housemate.entities.Service;
import housemate.entities.ServiceType;
import housemate.models.ServiceNewDTO;
import housemate.models.ServiceViewDTO;
import housemate.repositories.CommentRepository;
import housemate.repositories.FeedbackRepository;
import housemate.repositories.ImageRepository;
import housemate.repositories.PackageServiceItemRepository;
import housemate.repositories.PeriodPriceConfigRepository;
import housemate.repositories.PeriodRepository;
import housemate.repositories.ServiceConfigRepository;
import housemate.repositories.ServiceRepository;
import housemate.repositories.ServiceTypeRepository;
import housemate.utils.AuthorizationUtil;
import housemate.utils.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import housemate.models.ServiceViewDTO.ServicePrice;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 *
 * @author Anh
 */

@Component
public class TheService {

        @Autowired
        private ServiceRepository serviceRepo;
    
        @Autowired
        private ServiceTypeRepository serviceTypeRepo;
    
        @Autowired
        private PackageServiceItemRepository packageServiceItemRepo;
    
        @Autowired
        private CommentRepository commentRepo;
    
        @Autowired
        private FeedbackRepository feedbackRepo;
    
        @Autowired
        private PeriodRepository periodRepo;
    
        @Autowired
        private ImageRepository imgRepo;
    
        @Autowired
        private PeriodPriceConfigRepository periodPriceConfRepo;
    
        @Autowired
        private AuthorizationUtil authorizationUtil;
    
        @Autowired
        private ServiceConfigRepository servConfRepo;

	private ModelMapper mapper = new ModelMapper();
	
	private final ZoneId dateTimeZone = ZoneId.of("Asia/Ho_Chi_Minh");

	public ResponseEntity<?> getAllSingleService() {
	    List<Service> serviceList = List.of();
	    serviceList = serviceRepo.findAllByIsPackageFalse();
	    serviceList.forEach(s -> s.setImages(
		    imgRepo.findAllByEntityIdAndImageType(s.getServiceId(), ImageType.SERVICE).orElse(List.of())));
	    return ResponseEntity.ok().body(serviceList);
	}

	public ResponseEntity<?> getTopsale() {
	    List<Service> serviceList  = serviceRepo.findTopSale();
	    if (!serviceList.isEmpty())
		serviceList.forEach(s -> s.setImages(
			imgRepo.findAllByEntityIdAndImageType(s.getServiceId(), ImageType.SERVICE).orElse(List.of())));
	    return ResponseEntity.ok().body(serviceList);
	}
	
	public ResponseEntity<?> searchFilterAllKind(
			HttpServletRequest request,
			Optional<String> keyword,
			Optional<ServiceCategory> category,
			Optional<SaleStatus> saleStatus,
			Optional<Integer> rating,
			Optional<ServiceField> sortBy,
			Optional<SortRequired> orderBy,
			Optional<Integer> page,
			Optional<Integer> size) {

		// check the role admin is allowed
		if (!authorizationUtil.getRoleFromAuthorizationHeader(request).equals(Role.ADMIN.toString()))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");

		String keywordValue = !keyword.isPresent() ? null : StringUtil.stringNormalizationForCompare(keyword.get());
		Boolean categoryValue = category.isEmpty() 
				              ? null
				              : (category.get().equals(ServiceCategory.PACKAGE) == true ? true : false);
		SaleStatus statusValue = saleStatus.orElse(null);
		int ratingValue = rating.orElse(0);
		ServiceField fieldname = sortBy.orElse(ServiceField.PRICE);
		SortRequired requireOrder = orderBy.orElse(SortRequired.ASC);
		int pageNo = page.orElse(0);
		int pageSize = size.orElse(9);
		if (pageNo < 0 || pageSize < 1)
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Page number starts with 1. Page size must not be less than 1");

		// setting sort
		Sort sort;
		sort = Sort.by(Sort.Direction.ASC, fieldname.getFieldName());
		if (requireOrder.equals(SortRequired.DESC))
			sort = Sort.by(Sort.Direction.DESC, fieldname.getFieldName());

		Pageable sortedPage = pageNo == 0 ? PageRequest.of(0, pageSize, sort)
										  : PageRequest.of(pageNo - 1, pageSize, sort);

		Page<Service> serviceList = serviceRepo.searchFilterAllKind(statusValue, keywordValue, ratingValue, categoryValue, sortedPage);
		int maxPages = (int) Math.ceil((double) serviceList.getTotalPages());
		
		if (!serviceList.isEmpty())
		    serviceList.forEach(s -> s.setImages(imgRepo
			    .findAllByEntityIdAndImageType(s.getServiceId(), ImageType.SERVICE).orElse(List.of())));
		
		List<ServiceViewDTO> serviceViewList = new ArrayList<>();
		for (Service service : serviceList.getContent()) {
				ServiceViewDTO serviceView = new ServiceViewDTO();
				//set service to view
				serviceView.setService(service);
				List<ServicePrice> priceList = new ArrayList<>();
				List<Period> periodServiceList = periodRepo.findAllByServiceId(service.getServiceId());
				periodServiceList.forEach(s -> priceList.add(mapper.map(s, ServicePrice.class)));
				// Sort priceList by periodValue (month) in ascending order
				Collections.sort(priceList, Comparator.comparing(ServicePrice::getPeriodValue));
				serviceView.setPriceList(priceList);
				serviceViewList.add(serviceView);
		}
		Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
		Page<ServiceViewDTO> serviceViewPage = new PageImpl<ServiceViewDTO>(serviceViewList, serviceList.getPageable(), serviceList.getTotalElements());

		return ResponseEntity.ok(serviceViewPage);
	}

	public ResponseEntity<?> searchFilterAllKindAvailable(
			String keyword,
			Optional<ServiceCategory> category,
			Optional<SaleStatus> saleStatus,
			Optional<Integer> rating,
			Optional<ServiceField> sortBy,
			Optional<SortRequired> orderBy,
			Optional<Integer> page,
			Optional<Integer> size) {

		String keywordValue = keyword == null ? null : StringUtil.stringNormalizationForCompare(keyword);
		Boolean categoryValue = category.isEmpty() ? null : (category.get().equals(ServiceCategory.PACKAGE) == true ? true : false);
		SaleStatus statusValue = saleStatus.orElse(null);
		int ratingValue = rating.orElse(0);
		ServiceField fieldname = sortBy.orElse(ServiceField.PRICE);
		SortRequired requireOrder = orderBy.orElse(SortRequired.ASC);
		int pageNo = page.orElse(0);
		int pageSize = size.orElse(9);

		if (pageNo < 0 || pageSize < 1)
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Page number starts with 1. Page size must not be less than 1");
		
		// setting sort
		Sort sort;
		sort = Sort.by(Sort.Direction.ASC, fieldname.getFieldName());
		if (requireOrder.equals(SortRequired.DESC))
			sort = Sort.by(Sort.Direction.DESC, fieldname.getFieldName());

		Pageable sortedPage = pageNo == 0 ? PageRequest.of(0, pageSize, sort)
				                          : PageRequest.of(pageNo - 1, pageSize, sort);

		Page<Service> serviceList = serviceRepo.searchFilterAllAvailable(statusValue, keywordValue, ratingValue, categoryValue, sortedPage);
		int maxPages = (int) Math.ceil((double) serviceList.getTotalPages());
		
		if (!serviceList.isEmpty())
		    serviceList.forEach(s -> s.setImages(imgRepo
			    .findAllByEntityIdAndImageType(s.getServiceId(), ImageType.SERVICE).orElse(List.of())));
		
		return ResponseEntity.ok(serviceList);
	}

	public ResponseEntity<?> getOne(int serviceId) {

		ServiceViewDTO serviceDtoForDetail = new ServiceViewDTO();

		Service service = serviceRepo.findById(serviceId).orElse(null);
		List<Image> imgList = imgRepo.findAllByEntityIdAndImageType(serviceId, ImageType.SERVICE).orElse(List.of());
		service.setImages(imgList);
		
		if (service == null)
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found this service !");

		service.setNumberOfReview(feedbackRepo.findAllByServiceId(serviceId).size());
		service.setNumberOfComment(commentRepo.getAllCommentAndReplyByServiceId(serviceId));
		serviceDtoForDetail.setService(service);

		if (!service.isPackage()) { // this is a service
			List<ServiceType> typeList = serviceTypeRepo.findAllByServiceId(service.getServiceId()).orElse(null);
			if (typeList != null)
				serviceDtoForDetail.setTypeList(typeList);
		} else if (service.isPackage()) { // this is a package
			List<PackageServiceItem> packageServiceChildList = packageServiceItemRepo
					.findAllByPackageServiceId(service.getServiceId()).orElse(null);
			if (packageServiceChildList != null) {
				for (PackageServiceItem packageServiceItem : packageServiceChildList) {
					packageServiceItem.setTypeList(serviceTypeRepo.findAllByServiceId(packageServiceItem.getSingleServiceId()).orElse(null));
					packageServiceItem.setImages(imgRepo.findAllByEntityIdAndImageType(packageServiceItem.getSingleServiceId(), ImageType.SERVICE).orElse(List.of()));
					
				}
				serviceDtoForDetail.setPackageServiceItemList(packageServiceChildList);
			}
		}

		// set combo price for each service
		List<ServicePrice> priceList = new ArrayList<>();
		ServicePrice servicePrice = new ServicePrice();
		List<Period> periodServiceList = periodRepo.findAllByServiceId(serviceId);
		periodServiceList.forEach(s -> priceList.add(mapper.map(s, ServicePrice.class)));
		serviceDtoForDetail.setPriceList(priceList);
		
		return ResponseEntity.ok().body(serviceDtoForDetail);
	}

	@Transactional
	public ResponseEntity<?> createNew(HttpServletRequest request, ServiceNewDTO serviceDTO) {

		// check the role admin is allowed
		if (!authorizationUtil.getRoleFromAuthorizationHeader(request).equals(Role.ADMIN.toString()))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");

		Service savedService = null;

		try {
			// Check all before saving object service
			// Check duplicate title name
			String formatedTitleName = StringUtil.formatedString(serviceDTO.getTitleName());
			serviceDTO.setTitleName(formatedTitleName);
			if (serviceRepo.findByTitleNameIgnoreCase(StringUtil.removeDiacriticalMarks(formatedTitleName)) != null)
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The title name has existed before !");

			//check valid between original price and final price of service 
			if (serviceDTO.getFinalPrice() > serviceDTO.getOriginalPrice())
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("Set the Final Price from 0 to upper, smaller than or equal Original Price ");

			// Set auto sale status
			if (serviceDTO.getSaleStatus().equals(SaleStatus.DISCONTINUED))
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("Create new service the sale status must be Onsale or Available");
			else if ((serviceDTO.getOriginalPrice() - serviceDTO.getFinalPrice()) > 0)
				serviceDTO.setSaleStatus(SaleStatus.ONSALE);
			else
				serviceDTO.setSaleStatus(SaleStatus.AVAILABLE);
			
			//check the group name is included in SERVICE_GROUP COLLECTION
			if(servConfRepo.findByConfigTypeAndConfigValue(ServiceConfiguration.SERVICE_GROUPS.name(), StringUtil.formatedString(serviceDTO.getGroupType())).orElse(null) == null) {
				return ResponseEntity.badRequest()
						.body("The " + ServiceConfiguration.SERVICE_GROUPS.name() + " does not include the config value " 
								+ StringUtil.formatedString(serviceDTO.getGroupType()).toUpperCase()
								+ " .Let config new value for " + ServiceConfiguration.SERVICE_GROUPS.name());
			}
			//check the unit of measure is included in SERVICE_GROUP COLLECTION
			if(servConfRepo.findByConfigTypeAndConfigValue(ServiceConfiguration.SERVICE_UNITS.name(), StringUtil.formatedString(serviceDTO.getUnitOfMeasure())).orElse(null) == null) {
				return ResponseEntity.badRequest()
						.body("The " + ServiceConfiguration.SERVICE_UNITS.name() + " does not include the config value " 
								+ StringUtil.formatedString(serviceDTO.getUnitOfMeasure()).toUpperCase()
								+ " .Let config new value for " + ServiceConfiguration.SERVICE_UNITS.name());
			}


			// check single service constraints
			if (!serviceDTO.getIsPackage()) {
				if (serviceDTO.getServiceChildList() != null)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("The single service not allow to set service child list !");
				if (serviceDTO.getUnitOfMeasure().equals("Gói"))
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("The unit of measure of single service should not be Gói !");
				if (serviceDTO.getTypeNameList() != null) {
					Set<String> typeNameList = serviceDTO.getTypeNameList();
					Set<String> uniqueNames = new HashSet<>();
					// check any type name have equal ignore case
					for (String typeName : typeNameList)
						if (!uniqueNames.add(StringUtil.stringNormalizationForCompare(typeName)))
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("Duplicated the type name in this service !");
				}
			}

			// check package constraints
			if (serviceDTO.getIsPackage()) {
				if (serviceDTO.getTypeNameList() != null)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("The package not allow to set type name list !");
				if (serviceDTO.getServiceChildList() == null || serviceDTO.getServiceChildList().size() < 2)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("The package contains at least 2 single services !");
				if (!serviceDTO.getUnitOfMeasure().equals("Gói"))
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("The unit of measure of package has been set default to Gói !");
				for (Integer singleServiceId : serviceDTO.getServiceChildList().keySet()) {
					if (serviceRepo.findByServiceId(singleServiceId).isEmpty())
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
								"Single service child id " + singleServiceId + " does not existing in provided list !");
					if (serviceDTO.getServiceChildList().get(singleServiceId) <= 0)
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body("The quantity of single child service must greater than 0 !");
				}
			}

			// check service price cycle list constraints
			List<Integer> cycleList = List.of(3, 6, 9, 12);
			Map<Integer, Integer> cylcePriceListOfNewServ = serviceDTO.getPeriodPriceServiceList();
			if (cylcePriceListOfNewServ.size() != 4 || !cycleList.containsAll(cylcePriceListOfNewServ.keySet()))
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("Have to set price foreach 4 cycles : 3, 6, 9 ,12 of this service");
			Function<Integer, PeriodPriceConfig> findByConfigValueAndConfigName = key -> periodPriceConfRepo
					.findByConfigValueAndConfigName(LocalDateTime.now(dateTimeZone), key, TimeUnit.MONTH);
			for (Entry<Integer, Integer> period : cylcePriceListOfNewServ.entrySet()) {
				PeriodPriceConfig periodPriceConfig = findByConfigValueAndConfigName.apply(period.getKey());
				if (periodPriceConfig != null) {
					float min = periodPriceConfig.getMin();
					float max = periodPriceConfig.getMax();
					float periodPrice = period.getValue();
					float propor = periodPrice / serviceDTO.getFinalPrice();
					boolean validSetting = min <= propor && propor <= max;
					if (!validSetting) {
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body("Period price for cycle " + period.getKey() + " out of range proportion [" + min
										+ "-" + max + "]" + " ~ [" + min * serviceDTO.getOriginalPrice() + "-"
										+ max * serviceDTO.getOriginalPrice() + "]" + " against the original price "
										+ serviceDTO.getOriginalPrice());
					}
				}
			}
			cylcePriceListOfNewServ.put(1, serviceDTO.getFinalPrice()); // the cycle 1 into the service period price

			// ==after check all then map to DTO & save SavedService into DB to get newservice Id==
			savedService = serviceRepo.save(mapper.map(serviceDTO, Service.class));
			
			if (savedService == null)
				return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Something Error ! Saved Failed !");

			// save typeNameList for single services after saving Service object success
			if (serviceDTO.getTypeNameList() != null && !serviceDTO.getIsPackage() && savedService != null) {
				int savedServiceId = savedService.getServiceId();
				for (String element : serviceDTO.getTypeNameList()) {
					ServiceType type = new ServiceType();
					type.setServiceId(savedServiceId);
					type.setTypeName(element.trim().replaceAll("\\s+", " "));
					serviceTypeRepo.save(type);
				}
			}

			// save child service for package after saving Service Object success
			if (serviceDTO.getServiceChildList() != null && serviceDTO.getIsPackage() && savedService != null) {
				int savedServiceId = savedService.getServiceId();
				Map<Integer, Integer> childServiceSet = serviceDTO.getServiceChildList();
				int sumSingleServiceOriginalPrice = 0;
				for (Integer singleServiceId : childServiceSet.keySet()) {
					PackageServiceItem item = new PackageServiceItem(); // save package service item
					item.setPackageServiceId(savedServiceId);
					item.setSingleServiceId(singleServiceId);
					item.setQuantity(childServiceSet.get(singleServiceId));
					sumSingleServiceOriginalPrice += (serviceRepo.findByServiceId(singleServiceId).orElse(null)
							                      .getOriginalPrice() * item.getQuantity());
					packageServiceItemRepo.save(item);
				}

				// check original price of package
				if (serviceDTO.getOriginalPrice() != sumSingleServiceOriginalPrice) {
					TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("The original price of package must be the sum of all single service child list !"
							    + "\nThe original price of package should be " + sumSingleServiceOriginalPrice);
				}
				
				//Auto saving sum originalPrice
				savedService.setOriginalPrice(sumSingleServiceOriginalPrice);
				serviceRepo.save(savedService);
			}
			
			// save the cycle price list
			for (Integer cycleVaule : cylcePriceListOfNewServ.keySet()) {
				float cyclePropor = 1;
				if (cycleVaule != 1) {
					cyclePropor = periodPriceConfRepo
							.findByConfigValueAndConfigName(LocalDateTime.now(dateTimeZone), cycleVaule, TimeUnit.MONTH)
							.getMax();
				}
				Period newServicePeriod = Period.builder().serviceId(savedService.getServiceId())
						.periodValue(cycleVaule).periodName(TimeUnit.MONTH.name())
						.finalPrice(cylcePriceListOfNewServ.get(cycleVaule))
						.originalPrice((int) Math.ceil(savedService.getOriginalPrice() * cyclePropor)).build();
				periodRepo.save(newServicePeriod);
			}

		} catch (Exception e) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Something Error ! Saved Failed !");
		}

		return getOne(savedService.getServiceId());
	}

	@Transactional
	public ResponseEntity<?> updateInfo(HttpServletRequest request, int serviceId, ServiceNewDTO serviceDTO) {

		// check the role admin is allowed
		if (!authorizationUtil.getRoleFromAuthorizationHeader(request).equals(Role.ADMIN.toString()))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");

		Service updatedService = null;

		try {
			Service oldService = serviceRepo.findById(serviceId).orElse(null);
			if (oldService == null)
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The service does not exists !");

			// Check Valid Name For Update
			String formatedTitleName = StringUtil.formatedString(serviceDTO.getTitleName());
			serviceDTO.setTitleName(formatedTitleName);
			if (!formatedTitleName.equalsIgnoreCase(oldService.getTitleName()))
				if (serviceRepo.findByTitleNameIgnoreCase(StringUtil.removeDiacriticalMarks(formatedTitleName)) != null)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The title name has existed before !");
			
			//check valid between original price and final price of service 
			if (serviceDTO.getFinalPrice() > serviceDTO.getOriginalPrice())
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("Set the Final Price from 0 to upper and smaller than or equal Original Price ");

			if (serviceDTO.getSaleStatus().equals(SaleStatus.DISCONTINUED))
				oldService.setSaleStatus(SaleStatus.DISCONTINUED);
			else if ((serviceDTO.getOriginalPrice() - serviceDTO.getFinalPrice()) > 0)
				oldService.setSaleStatus(SaleStatus.ONSALE);
			else
				oldService.setSaleStatus(SaleStatus.AVAILABLE);

			//check not allow to update the unit of measure
			if (!serviceDTO.getUnitOfMeasure().equals(oldService.getUnitOfMeasure()))
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("Not allow to update the unit of measure. Please correct the unit of measure to " + oldService.getUnitOfMeasure());
			
			// check is package
			if (oldService.isPackage() && serviceDTO.getIsPackage().equals(false))
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("This sevice id is the package service. Not allow to change the status of is package !"
								+ " \nSet this isPackage to be true please");
			else if (!oldService.isPackage() && serviceDTO.getIsPackage().equals(true))
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("This sevice id is the single service. Not allow to change the status of is package !"
								+ " \nSet this isPackage to be false please");
			
			//check the group name is included in SERVICE_GROUP COLLECTION
			if(servConfRepo.findByConfigTypeAndConfigValue(ServiceConfiguration.SERVICE_GROUPS.name(), StringUtil.formatedString(serviceDTO.getGroupType())).orElse(null) == null) {
				return ResponseEntity.badRequest()
						.body("The " + ServiceConfiguration.SERVICE_GROUPS.name() + " does not include the config value " 
								+ StringUtil.formatedString(serviceDTO.getGroupType()).toUpperCase()
								+ " .Let config new value for " + ServiceConfiguration.SERVICE_GROUPS.name());
			}


			// update typeNameList for single services
			if (!oldService.isPackage()) {
				if (serviceDTO.getServiceChildList() != null)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("This sevice id is the single service. Not allow to set service child list !");
				if (serviceDTO.getTypeNameList().isEmpty())
					serviceTypeRepo.updateServiceIdForRemove(serviceId);
				// check type name of each single service is unique ignore case after request
				if (!serviceDTO.getTypeNameList().isEmpty()) {
					Set<String> typeNameList = serviceDTO.getTypeNameList();
					Set<String> uniqueNames = new HashSet<>();
					// check any type name have equal ignore case
					for (String typeName : typeNameList)
						if (!uniqueNames.add(StringUtil.stringNormalizationForCompare(typeName)))
							return ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body("Duplicated the type name of this service !");
					// Reset Type Name List and Update
					serviceTypeRepo.updateServiceIdForRemove(serviceId);
					for (String element : serviceDTO.getTypeNameList()) {
						ServiceType type = new ServiceType();
						type.setServiceId(serviceId);
						type.setTypeName(element);
						serviceTypeRepo.save(type);
					}
				}
			}

			// check single service id existed in db
			if (oldService.isPackage()) {
				if (serviceDTO.getTypeNameList() != null)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("The package not allow to set type name list !");
				if (serviceDTO.getServiceChildList() == null || serviceDTO.getServiceChildList().size() < 2)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("The package contains at least 2 single services !");
				Map<Integer, Integer> childServiceSet = serviceDTO.getServiceChildList();
				for (Integer singleServiceId : childServiceSet.keySet()) {
					if (packageServiceItemRepo.findByPackageServiceIdAndSingleServiceId(serviceId, singleServiceId)
							.isEmpty())
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The single service id "
								+ singleServiceId + " is not allow in here !"
								+ " \nNot allow to change the existing single service item list in this package !");
					if (serviceDTO.getServiceChildList().get(singleServiceId) <= 0)
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body("The new quantity of single child service must greater than 0 !");
					PackageServiceItem item = packageServiceItemRepo
							.findByPackageServiceIdAndSingleServiceId(serviceId, singleServiceId).orElse(null);
					item.setQuantity(childServiceSet.get(singleServiceId));
					packageServiceItemRepo.save(item);
				}
			}
			
			// check typename list and single service list ok then save all into db
			oldService.setTitleName(serviceDTO.getTitleName());
			oldService.setDescription(serviceDTO.getDescription());
			oldService.setOriginalPrice(serviceDTO.getOriginalPrice());
			oldService.setFinalPrice(serviceDTO.getFinalPrice());
			oldService.setGroupType(serviceDTO.getGroupType());
			oldService.setMin(serviceDTO.getMin());
			oldService.setMax(serviceDTO.getMax());
			updatedService = serviceRepo.save(oldService);
			
			//Update the price cycle list of this service
			// check service price cycle list constraints
			List<Integer> cycleList = List.of(3, 6, 9, 12);
			Map<Integer, Integer> cylcePriceListOfNewServ = serviceDTO.getPeriodPriceServiceList();
			if (cylcePriceListOfNewServ.size() != 4 || !cycleList.containsAll(cylcePriceListOfNewServ.keySet()))
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("Have to set price foreach 4 cycles : 3, 6, 9 ,12 of this service");
			Function<Integer, PeriodPriceConfig> findByConfigValueAndConfigName = key -> periodPriceConfRepo
					.findByConfigValueAndConfigName(LocalDateTime.now(dateTimeZone), key, TimeUnit.MONTH);
			for (Entry<Integer, Integer> period : cylcePriceListOfNewServ.entrySet()) {
				PeriodPriceConfig periPriceConfig = findByConfigValueAndConfigName.apply(period.getKey());
				if (periPriceConfig != null) {
					float min = periPriceConfig.getMin();
					float max = periPriceConfig.getMax();
					float periodPrice = period.getValue();
					float propor = periodPrice / serviceDTO.getFinalPrice();
					boolean validSetting = min <= propor && propor <= max;
					if (!validSetting) {
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body("Period price for cycle " + period.getKey() + " out of range proportion [" + min
										+ "-" + max + "]" + " ~ [" + min * serviceDTO.getOriginalPrice() + "-"
										+ max * serviceDTO.getOriginalPrice() + "]" + " against the original price "
										+ serviceDTO.getOriginalPrice());
					}
				}
			}
			cylcePriceListOfNewServ.put(1, serviceDTO.getFinalPrice()); // the cycle 1 into the service period price

			for (Integer cycleVaule : cylcePriceListOfNewServ.keySet()) {
				float cyclePropor = 1;
				if (cycleVaule != 1) {
					cyclePropor = periodPriceConfRepo
							.findByConfigValueAndConfigName(LocalDateTime.now(dateTimeZone), cycleVaule, TimeUnit.MONTH)
							.getMax();
				}
				periodRepo.save(Period.builder()
						.periodId(periodRepo.findByServiceIdAndPeriodValue(oldService.getServiceId(),cycleVaule).getPeriodId())
						.serviceId(oldService.getServiceId())
						.periodValue(cycleVaule)
						.periodName(TimeUnit.MONTH.name())
						.finalPrice(cylcePriceListOfNewServ.get(cycleVaule))
						.originalPrice((int) Math.ceil(updatedService.getOriginalPrice() * cyclePropor))
						.build());
			}

		} catch (Exception e) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Something Error ! Update Failed ! ");
		}

		return this.getOne(updatedService.getServiceId());
	}

	// TODO: DELETE SERVICE LATER
	  

}
