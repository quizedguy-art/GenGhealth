import './privacy.css';

export const metadata = {
  title: 'Privacy Policy - GenGhealth',
  description: 'How we protect your data at GenGhealth',
};

export default function PrivacyPage() {
  return (
    <div className="privacy-container">
      <h1>Privacy Policy for GenGhealth</h1>
      <p className="last-updated">Last Updated: March 29, 2026</p>

      <section>
        <h2>1. Information We Collect</h2>
        <p><strong>Screen Usage Data:</strong> We track your screen time and usage statistics locally on your device. This data is used solely to calculate rewards and is <strong>never</strong> uploaded to our servers.</p>
        <p><strong>Personal Information:</strong> We collect your name and email address during registration to manage your account and process reward redemptions.</p>
      </section>

      <section>
        <h2>2. How We Use Information</h2>
        <ul>
          <li>To monitor screen time for reward eligibility.</li>
          <li>To process withdrawal requests for gift cards/vouchers.</li>
          <li>To provide support via quizedguy@gmail.com.</li>
        </ul>
      </section>

      <section>
        <h2>3. Advertisements (AdMob)</h2>
        <p>We use Google AdMob to serve advertisements. AdMob may collect and use data about your device and ad interactions to provide interest-based advertising. You can manage your ad preferences in your Google Account settings.</p>
      </section>

      <section>
        <h2>4. Data Security</h2>
        <p>Your authentication data is secured by Firebase Auth, and your points/redemption history is protected in our private Firestore database. We do not share your personal information with third parties.</p>
      </section>

      <section>
        <h2>5. Contact Us</h2>
        <p>If you have any questions about this Privacy Policy, please contact us at:</p>
        <p><strong>Email:</strong> quizedguy@gmail.com</p>
      </section>
    </div>
  );
}
