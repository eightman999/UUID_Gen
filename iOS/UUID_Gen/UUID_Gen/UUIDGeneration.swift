import CryptoKit
import Foundation
import Security

enum UUIDVersion: String, CaseIterable, Identifiable {
    case v4 = "v4"
    case v5 = "v5"
    case v7 = "v7"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .v4: return "UUID v4"
        case .v5: return "UUID v5"
        case .v7: return "UUID v7"
        }
    }

    var description: String {
        switch self {
        case .v4: return "ランダム値ベース"
        case .v5: return "名前空間 + 名前 (SHA-1)"
        case .v7: return "時刻ベース (RFC 9562)"
        }
    }
}

struct UuidFormatOptions: OptionSet {
    let rawValue: Int64

    static let hyphen = UuidFormatOptions(rawValue: 1 << 0)
    static let uppercase = UuidFormatOptions(rawValue: 1 << 1)
    static let braces = UuidFormatOptions(rawValue: 1 << 2)

    static let `default`: UuidFormatOptions = [.hyphen]
}

struct GeneratedUuid: Identifiable {
    let id: String
    let value: UUID
    let version: UUIDVersion
    let createdAt: Date
    let namespace: UUID?
    let name: String?
    let format: UuidFormatOptions

    init(value: UUID, version: UUIDVersion, createdAt: Date = Date(), namespace: UUID? = nil, name: String? = nil, format: UuidFormatOptions = .default) {
        self.id = value.uuidString
        self.value = value
        self.version = version
        self.createdAt = createdAt
        self.namespace = namespace
        self.name = name
        self.format = format
    }
}

enum UUIDGenerationError: Error {
    case invalidNamespace
    case invalidName
}

enum UUIDGenerator {
    static func generate(version: UUIDVersion, namespaceString: String?, name: String?, now: Date = Date()) throws -> GeneratedUuid {
        switch version {
        case .v4:
            return GeneratedUuid(value: UUID(), version: .v4, createdAt: now)
        case .v5:
            guard let namespaceString, let namespace = UUID(uuidString: namespaceString) else {
                throw UUIDGenerationError.invalidNamespace
            }
            guard let name, !name.isEmpty else {
                throw UUIDGenerationError.invalidName
            }
            let uuid = uuidV5(namespace: namespace, name: name)
            return GeneratedUuid(value: uuid, version: .v5, createdAt: now, namespace: namespace, name: name)
        case .v7:
            let uuid = uuidV7(now: UInt64(now.timeIntervalSince1970 * 1000))
            return GeneratedUuid(value: uuid, version: .v7, createdAt: now)
        }
    }

    static func uuidV5(namespace: UUID, name: String) -> UUID {
        let namespaceBytes = withUnsafeBytes(of: namespace.uuid) { Data($0) }
        let nameBytes = Data(name.utf8)
        var data = Data()
        data.append(namespaceBytes)
        data.append(nameBytes)

        let hash = Insecure.SHA1.hash(data: data)
        var bytes = Array(hash)

        bytes[6] = (bytes[6] & 0x0F) | 0x50
        bytes[8] = (bytes[8] & 0x3F) | 0x80

        return UUID(uuid: (bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7], bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]))
    }

    static func uuidV7(now: UInt64) -> UUID {
        var randomBytes = [UInt8](repeating: 0, count: 10)
        let status = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        if status != errSecSuccess {
            for i in 0..<randomBytes.count {
                randomBytes[i] = UInt8.random(in: UInt8.min...UInt8.max)
            }
        }

        var bytes = [UInt8](repeating: 0, count: 16)
        bytes[0] = UInt8((now >> 40) & 0xFF)
        bytes[1] = UInt8((now >> 32) & 0xFF)
        bytes[2] = UInt8((now >> 24) & 0xFF)
        bytes[3] = UInt8((now >> 16) & 0xFF)
        bytes[4] = UInt8((now >> 8) & 0xFF)
        bytes[5] = UInt8(now & 0xFF)

        bytes[6] = (randomBytes[0] & 0x0F) | 0x70
        bytes[7] = randomBytes[1]

        bytes[8] = (randomBytes[2] & 0x3F) | 0x80
        bytes[9] = randomBytes[3]

        for i in 0..<6 {
            bytes[10 + i] = randomBytes[4 + i]
        }

        return UUID(uuid: (bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7], bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]))
    }
}

func format(uuid: UUID, options: UuidFormatOptions) -> String {
    var string = uuid.uuidString
    if !options.contains(.hyphen) {
        string = string.replacingOccurrences(of: "-", with: "")
    }
    if options.contains(.uppercase) {
        string = string.uppercased()
    } else {
        string = string.lowercased()
    }
    if options.contains(.braces) {
        string = "{\(string)}"
    }
    return string
}
