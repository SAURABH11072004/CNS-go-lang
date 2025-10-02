package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/PuerkitoBio/goquery"
)

// ScrapedData represents the structure of data we extract
type ScrapedData struct {
	URL         string   `json:"url"`
	Title       string   `json:"title"`
	Description string   `json:"description"`
	Headings    []string `json:"headings"`
}

// fetchPage fetches and scrapes a single URL
func fetchPage(url string) ScrapedData {
	// HTTP client with timeout
	client := &http.Client{Timeout: 15 * time.Second}

	// Create request with headers
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		log.Println("Error creating request:", err)
		return ScrapedData{URL: url, Title: "Error"}
	}
	req.Header.Set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")

	// Send request
	resp, err := client.Do(req)
	if err != nil {
		log.Println("Request failed:", err)
		return ScrapedData{URL: url, Title: "No Title Found"}
	}
	defer resp.Body.Close()

	// Parse HTML with goquery
	doc, err := goquery.NewDocumentFromReader(resp.Body)
	if err != nil {
		log.Println("Error parsing HTML:", err)
		return ScrapedData{URL: url, Title: "No Title Found"}
	}

	// Extract title
	title := doc.Find("title").First().Text()
	if title == "" {
		title = "No Title Found"
	}

	// Extract meta description
	description, _ := doc.Find("meta[name='description']").Attr("content")
	if description == "" {
		description = "No Description Found"
	}

	// Extract headings (h1, h2, h3)
	var headings []string
	doc.Find("h1, h2, h3").Each(func(i int, s *goquery.Selection) {
		headings = append(headings, s.Text())
	})

	return ScrapedData{
		URL:         url,
		Title:       title,
		Description: description,
		Headings:    headings,
	}
}

func main() {
	// ✅ Ask user for URLs
	var n int
	fmt.Print("Enter number of URLs: ")
	fmt.Scan(&n)

	urls := make([]string, n)
	for i := 0; i < n; i++ {
		fmt.Printf("Enter URL %d: ", i+1)
		fmt.Scan(&urls[i])
	}

	results := []ScrapedData{}

	// Run scraping concurrently
	done := make(chan ScrapedData)
	for _, url := range urls {
		go func(u string) {
			done <- fetchPage(u)
		}(url)
	}

	// Collect results
	for range urls {
		results = append(results, <-done)
	}

	// Print JSON result (pretty format)
	output, _ := json.MarshalIndent(results, "", "  ")
	fmt.Println(string(output))
}
