package main

import (
	"encoding/json"
	"log"
	"net/http"

	"golang.org/x/crypto/bcrypt"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

var db *gorm.DB

// User model
type User struct {
	ID       uint   `json:"id" gorm:"primaryKey"`
	Name     string `json:"name"`
	Email    string `json:"email" gorm:"unique"`
	Password string `json:"-"`
}

// Initialize database connection
func initDB() {
	// Use the same credentials as docker-compose.yml
	dsn := "host=localhost user=admin password=secret dbname=userdb port=5432 sslmode=disable"

	var err error
	db, err = gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Fatalf("❌ Failed to connect to database: %v", err)
	}

	// Auto migrate User model
	if err := db.AutoMigrate(&User{}); err != nil {
		log.Fatalf("❌ Failed to migrate User model: %v", err)
	}

	log.Println("✅ Database connected & migrated successfully!")
}

// Register a new user
func registerUser(w http.ResponseWriter, r *http.Request) {
	var user User
	if err := json.NewDecoder(r.Body).Decode(&user); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	// Hash the password
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(user.Password), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, "Error hashing password", http.StatusInternalServerError)
		return
	}
	user.Password = string(hashedPassword)

	// Save user to DB
	if err := db.Create(&user).Error; err != nil {
		http.Error(w, "❌ Could not create user", http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(user)
}

// Get all users
func getUsers(w http.ResponseWriter, r *http.Request) {
	var users []User
	db.Find(&users)
	json.NewEncoder(w).Encode(users)
}

// Update user
func updateUser(w http.ResponseWriter, r *http.Request) {
	var input User
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	var user User
	if err := db.First(&user, input.ID).Error; err != nil {
		http.Error(w, "❌ User not found", http.StatusNotFound)
		return
	}

	user.Name = input.Name
	user.Email = input.Email
	db.Save(&user)

	json.NewEncoder(w).Encode(user)
}

// Delete user
func deleteUser(w http.ResponseWriter, r *http.Request) {
	var input User
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	if err := db.Delete(&User{}, input.ID).Error; err != nil {
		http.Error(w, "❌ Could not delete user", http.StatusInternalServerError)
		return
	}

	w.Write([]byte("✅ User deleted successfully"))
}

// Main function
func main() {
	initDB()

	http.HandleFunc("/users", func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case "POST":
			registerUser(w, r)
		case "GET":
			getUsers(w, r)
		case "PUT":
			updateUser(w, r)
		case "DELETE":
			deleteUser(w, r)
		default:
			http.Error(w, "❌ Method not allowed", http.StatusMethodNotAllowed)
		}
	})

	log.Println("🚀 Server running on :8080")
	log.Fatal(http.ListenAndServe(":8080", nil))
}
