import CoreData
import Foundation

final class PersistenceController {
    static let shared = PersistenceController()

    let container: NSPersistentContainer

    init(inMemory: Bool = false) {
        let model = Self.makeModel()
        container = NSPersistentContainer(name: "UUIDGen", managedObjectModel: model)
        if inMemory {
            container.persistentStoreDescriptions.first?.url = URL(fileURLWithPath: "/dev/null")
        }
        container.loadPersistentStores { _, error in
            if let error = error {
                fatalError("Unresolved error: \(error)")
            }
        }
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        container.viewContext.automaticallyMergesChangesFromParent = true
    }

    private static func makeModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()

        // UuidItemEntity
        let uuidEntity = NSEntityDescription()
        uuidEntity.name = "UuidItemEntity"
        uuidEntity.managedObjectClassName = NSStringFromClass(UuidItemEntity.self)

        let uuidId = NSAttributeDescription()
        uuidId.name = "id"
        uuidId.attributeType = .stringAttributeType
        uuidId.isOptional = false

        let uuidValue = NSAttributeDescription()
        uuidValue.name = "value"
        uuidValue.attributeType = .stringAttributeType
        uuidValue.isOptional = false

        let uuidVersion = NSAttributeDescription()
        uuidVersion.name = "version"
        uuidVersion.attributeType = .stringAttributeType
        uuidVersion.isOptional = false

        let uuidCreatedAt = NSAttributeDescription()
        uuidCreatedAt.name = "createdAt"
        uuidCreatedAt.attributeType = .dateAttributeType
        uuidCreatedAt.isOptional = false

        let uuidLabel = NSAttributeDescription()
        uuidLabel.name = "label"
        uuidLabel.attributeType = .stringAttributeType
        uuidLabel.isOptional = true

        let uuidNamespace = NSAttributeDescription()
        uuidNamespace.name = "namespace"
        uuidNamespace.attributeType = .stringAttributeType
        uuidNamespace.isOptional = true

        let uuidName = NSAttributeDescription()
        uuidName.name = "name"
        uuidName.attributeType = .stringAttributeType
        uuidName.isOptional = true

        let uuidStyleFlags = NSAttributeDescription()
        uuidStyleFlags.name = "styleFlags"
        uuidStyleFlags.attributeType = .integer64AttributeType
        uuidStyleFlags.isOptional = false
        uuidStyleFlags.defaultValue = 0

        uuidEntity.properties = [uuidId, uuidValue, uuidVersion, uuidCreatedAt, uuidLabel, uuidNamespace, uuidName, uuidStyleFlags]

        // BonusSlotEntity
        let bonusEntity = NSEntityDescription()
        bonusEntity.name = "BonusSlotEntity"
        bonusEntity.managedObjectClassName = NSStringFromClass(BonusSlotEntity.self)

        let bonusId = NSAttributeDescription()
        bonusId.name = "id"
        bonusId.attributeType = .stringAttributeType
        bonusId.isOptional = false

        let bonusExpiresAt = NSAttributeDescription()
        bonusExpiresAt.name = "expiresAt"
        bonusExpiresAt.attributeType = .dateAttributeType
        bonusExpiresAt.isOptional = false

        bonusEntity.properties = [bonusId, bonusExpiresAt]

        // EntitlementEntity
        let entitlementEntity = NSEntityDescription()
        entitlementEntity.name = "EntitlementEntity"
        entitlementEntity.managedObjectClassName = NSStringFromClass(EntitlementEntity.self)

        let entitlementId = NSAttributeDescription()
        entitlementId.name = "id"
        entitlementId.attributeType = .stringAttributeType
        entitlementId.isOptional = false

        let entitlementIsPro = NSAttributeDescription()
        entitlementIsPro.name = "isPro"
        entitlementIsPro.attributeType = .booleanAttributeType
        entitlementIsPro.isOptional = false
        entitlementIsPro.defaultValue = false

        let entitlementProSince = NSAttributeDescription()
        entitlementProSince.name = "proSince"
        entitlementProSince.attributeType = .dateAttributeType
        entitlementProSince.isOptional = true

        let entitlementLastSync = NSAttributeDescription()
        entitlementLastSync.name = "lastSyncAt"
        entitlementLastSync.attributeType = .dateAttributeType
        entitlementLastSync.isOptional = true

        entitlementEntity.properties = [entitlementId, entitlementIsPro, entitlementProSince, entitlementLastSync]

        model.entities = [uuidEntity, bonusEntity, entitlementEntity]
        return model
    }
}

// MARK: - Managed Object subclasses

@objc(UuidItemEntity)
final class UuidItemEntity: NSManagedObject {
    @NSManaged var id: String
    @NSManaged var value: String
    @NSManaged var version: String
    @NSManaged var createdAt: Date
    @NSManaged var label: String?
    @NSManaged var namespace: String?
    @NSManaged var name: String?
    @NSManaged var styleFlags: Int64
}

@objc(BonusSlotEntity)
final class BonusSlotEntity: NSManagedObject {
    @NSManaged var id: String
    @NSManaged var expiresAt: Date
}

@objc(EntitlementEntity)
final class EntitlementEntity: NSManagedObject {
    @NSManaged var id: String
    @NSManaged var isPro: Bool
    @NSManaged var proSince: Date?
    @NSManaged var lastSyncAt: Date?
}
